import os
import tempfile
import torch
from fastapi import UploadFile
from contextlib import asynccontextmanager
from dotenv import load_dotenv

# 음성 AI 관련
from pyannote.audio import Pipeline
from transformers import WhisperProcessor, WhisperForConditionalGeneration, pipeline

# 텍스트 AI (LangChain) 관련
from langchain_openai import ChatOpenAI
from langchain_core.prompts import load_prompt
from voice_analyzer.schemas import MeetingAnalysis

load_dotenv()

# 전역 변수 및 LLM
diarization_pipeline = None
whisper_pipe = None

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
structured_llm = llm.with_structured_output(MeetingAnalysis)


# 서버 시작 시 실행되는 AI 모델
@asynccontextmanager
async def lifespan(app):
    global diarization_pipeline, whisper_pipe

    device_str = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"[서버 시작] AI 모델 로딩 중... (사용 엔진: {device_str.upper()})")

    # Pyannote 로딩
    HF_TOKEN = os.getenv("HF_TOKEN")
    diarization_pipeline = Pipeline.from_pretrained("pyannote/speaker-diarization-3.1", token=HF_TOKEN)
    if device_str == "cuda":
        diarization_pipeline.to(torch.device("cuda"))

    # Whisper 로딩
    MODEL_NAME = "openai/whisper-small"
    processor = WhisperProcessor.from_pretrained(MODEL_NAME, language="Korean", task="transcribe")
    base_model = WhisperForConditionalGeneration.from_pretrained(MODEL_NAME)

    whisper_pipe = pipeline(
        "automatic-speech-recognition",
        model=base_model,
        tokenizer=processor.tokenizer,
        feature_extractor=processor.feature_extractor,
        chunk_length_s=30,
        return_timestamps=True,
        device=0 if device_str == "cuda" else -1
    )

    print("[준비 완료] 음성 AI 서버가 정상 가동되었습니다.")
    yield
    print("[서버 종료] 자원을 해제합니다.")


# 음성 파일 -> 텍스트 대본 추출 함수
async def process_audio_to_text(file: UploadFile) -> str:
    temp_file_path = ""

    _, file_ext = os.path.splitext(file.filename)
    if not file_ext:
        file_ext = ".wav"  # 확장자가 없으면 기본값 적용

    # suffix에 무조건 .wav가 아닌 추출한 원본 확장자(file_ext) 넣음
    with tempfile.NamedTemporaryFile(delete=False, suffix=file_ext) as temp_audio:
        content = await file.read()
        temp_audio.write(content)
        temp_file_path = temp_audio.name

    try:
        print(f"[1단계] {file.filename} 화자 분리 및 음성 인식 시작...")

        # 전역 변수로 미리 켜둔 모델 가져오기
        global diarization_pipeline, whisper_pipe

        # [2단계: 읽기] 닫힌 파일을 AI 모델에게 안전하게 넘겨줍니다.
        diarization = diarization_pipeline(temp_file_path)
        result = whisper_pipe(temp_file_path)

        print("[2단계] 텍스트 및 화자 매칭 중...")

        tracks = list(diarization.speaker_diarization.itertracks(yield_label=True))
        full_transcript = ""

        for chunk in result["chunks"]:
            seg_start, seg_end = chunk["timestamp"]
            text = chunk["text"].strip()
            if seg_end is None:
                seg_end = seg_start + 2.0

            current_speaker = "Unknown"

            for turn, _, speaker in tracks:
                if turn.start <= seg_start + 0.5 and turn.end >= seg_start:
                    current_speaker = speaker
                    break

            full_transcript += f"[{current_speaker}] {text}\n"

        print(f"\n[중간 결과] 통합 대본:\n{full_transcript}\n")
        return full_transcript

    finally:
        # 모든 분석이 끝나면 삭제
        if os.path.exists(temp_file_path):
            try:
                os.remove(temp_file_path)
                print(f"[정리] 임시 오디오 파일 삭제 완료: {temp_file_path}")
            except Exception as e:
                print(f"[경고] 파일 삭제 실패 (무시 가능): {e}")

# 메인 실행 엔진
async def analyze_voice_meeting(file: UploadFile) -> dict:
    # 오디오 처리
    transcript = await process_audio_to_text(file)

    if not transcript or transcript.strip() == "":
        return {"status": "error", "message": "음성에서 텍스트를 추출하지 못했습니다."}

    print("[3단계] LangChain 기반 회의록 요약 분석 중...")

    # 프롬프트 로드 및 체인 실행
    prompt = load_prompt("voice_analyzer/prompts/meeting_summary.yaml", encoding="utf-8")
    chain = prompt | structured_llm

    final_result: MeetingAnalysis = await chain.ainvoke({"transcript": transcript})

    print("[4단계] 분석 완료!")

    # JSON 반환
    return {
        "status": "success",
        "data": final_result.model_dump()
    }
