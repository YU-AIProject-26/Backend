import os
import shutil
import torch
import librosa
from fastapi import UploadFile
from contextlib import asynccontextmanager
from transformers import WhisperProcessor, WhisperForConditionalGeneration

# 전역 변수로 무거운 AI 모델을 담아둘 공간
ml_models = {}

@asynccontextmanager
async def lifespan(app):
    """
    서버가 부팅될 때 딱 한 번 실행되는 구간입니다.
    (동업자의 server.py가 이 함수를 가져다 씁니다!)
    """
    print("🧠 STT AI 모델을 서버 메모리에 적재하는 중...")
    
    # 이사 온 위치(app 폴더)에 맞춘 완벽한 경로!
    current_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.abspath(os.path.join(current_dir, "../models/checkpoint-8000"))
    
    try:
        processor = WhisperProcessor.from_pretrained("openai/whisper-base")
        model = WhisperForConditionalGeneration.from_pretrained(model_path)
        device = "cuda" if torch.cuda.is_available() else "cpu"
        model.to(device)
        
        # 모델을 전역 변수에 저장
        ml_models["stt_processor"] = processor
        ml_models["stt_model"] = model
        ml_models["device"] = device
        
        print(f"✅ STT 모델 적재 완료! (사용 장치: {device})")
    except Exception as e:
        print(f"❌ 모델 적재 실패: 경로를 다시 확인해주세요. 에러내용: {e}")
    
    yield # --- 이 줄을 기점으로 서버가 돌아갑니다 ---
    
    # 서버가 꺼질 때 메모리 깔끔하게 비우기
    ml_models.clear()
    print("🛑 STT 모델 메모리 해제 완료")


async def analyze_voice_meeting(file: UploadFile):
    """
    스프링 부트에서 오디오 파일이 넘어오면 실제 분석을 수행하는 핵심 함수입니다.
    (동업자의 server.py가 이 함수를 가져다 씁니다!)
    """
    temp_file_path = f"temp_{file.filename}"
    
    # 1. 넘어온 파일을 서버에 임시 저장
    with open(temp_file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    try:
        print(f"🎧 STT 분석 시작: {file.filename}")
        processor = ml_models["stt_processor"]
        model = ml_models["stt_model"]
        device = ml_models["device"]
        
        # 2. 오디오 로드 및 16kHz 변환
        audio_array, sampling_rate = librosa.load(temp_file_path, sr=16000)
        
        # 3. 모델 추론
        inputs = processor(audio_array, sampling_rate=16000, return_tensors="pt").to(device)
        forced_decoder_ids = processor.get_decoder_prompt_ids(language="ko", task="transcribe")
        predicted_ids = model.generate(inputs["input_features"], forced_decoder_ids=forced_decoder_ids)
        result_text = processor.batch_decode(predicted_ids, skip_special_tokens=True)[0]
        
        print(f"✨ STT 텍스트 변환 완료!\n{result_text}")
        
        # 결과를 딕셔너리로 반환 (server.py에서 이 결과를 받아갑니다)
        return {
            "status": "success", 
            "stt_result": result_text,
            "message": "음성을 성공적으로 텍스트로 변환했습니다."
        }
        
    except Exception as e:
        print(f"❌ 분석 중 에러 발생: {e}")
        return {"status": "error", "message": str(e)}
        
    finally:
        # 4. 분석 끝난 임시 파일은 찌꺼기가 남지 않게 바로 삭제
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)