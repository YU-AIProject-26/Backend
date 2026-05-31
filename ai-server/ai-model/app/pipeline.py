import os
import re
import shutil
import torch
import librosa
from fastapi import UploadFile
from contextlib import asynccontextmanager
from transformers import WhisperProcessor, WhisperForConditionalGeneration

ml_models = {}

@asynccontextmanager
async def lifespan(app):
    print("STT AI 모델을 서버 메모리에 적재하는 중...")
    
    current_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.abspath(os.path.join(current_dir, "../models/checkpoint-8000"))
    
    try:
        processor = WhisperProcessor.from_pretrained("openai/whisper-base")
        model = WhisperForConditionalGeneration.from_pretrained(model_path)
        device = "cuda" if torch.cuda.is_available() else "cpu"
        model.to(device)
        
        ml_models["processor"] = processor
        ml_models["model"] = model
        ml_models["device"] = device
        
        print(f"STT 모델 적재 완료 (사용 장치: {device})")
    except Exception as e:
        print(f"모델 적재 실패: {e}")
    
    yield 
    
    ml_models.clear()
    print("STT 모델 메모리 해제 완료")


async def analyze_voice_meeting(file: UploadFile):
    temp_file_path = f"temp_{file.filename}"
    
    with open(temp_file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    try:
        print(f"STT 수동 분할(Stride 적용) 분석 시작: {file.filename}")
        processor = ml_models["processor"]
        model = ml_models["model"]
        device = ml_models["device"]
        
        # 1. 오디오 로드
        audio_array, sampling_rate = librosa.load(temp_file_path, sr=16000)
        
        # 💡 [최적화 적용] 30초 단위로 자르되, 앞뒤로 2초씩 겹치게(Stride) 설정
        chunk_length = 30 * 16000
        stride_length = 2 * 16000 
        step_size = chunk_length - stride_length # 실제 이동하는 보폭은 28초
        
        full_text = ""
        forced_decoder_ids = processor.get_decoder_prompt_ids(language="ko", task="transcribe")
        
        print("30초 단위(2초 겹침)로 해독을 시작합니다...")
        
        for i in range(0, len(audio_array), step_size):
            chunk = audio_array[i : i + chunk_length]
            
            # 자투리가 1초 미만이면 무시
            if len(chunk) < 16000:
                break
                
            inputs = processor(chunk, sampling_rate=16000, return_tensors="pt").to(device)
            predicted_ids = model.generate(
                inputs["input_features"], 
                forced_decoder_ids=forced_decoder_ids,
                repetition_penalty=1.2
            )

            chunk_text = processor.batch_decode(predicted_ids, skip_special_tokens=True)[0]
            
            chunk_text = chunk_text.replace("(())", " ") # 괄호 찌꺼기 제거
            chunk_text = re.sub(r'(.)\1{4,}', r'\1', chunk_text) # 똑같은 글자가 5번 이상 연속되면 압축
            chunk_text = re.sub(r' +', ' ', chunk_text).strip() # 다중 공백 제거
            
            # [중복 방지 가벼운 후처리] 2초가 겹치면서 단어가 두 번 출력되는 현상 최소화
            if full_text and chunk_text:
                words_full = full_text.split()
                words_chunk = chunk_text.split()
                # 이전 문장의 마지막 단어와 새 문장의 첫 단어가 겹치면 하나 잘라내기
                if words_full and words_chunk and words_full[-1] == words_chunk[0]:
                    chunk_text = " ".join(words_chunk[1:])
            
            full_text += chunk_text + " "
            print(f"[진행중] 추출됨: {chunk_text}")
            
        print("\n모든 오디오 변환 완료!")
        
        return {
            "status": "success", 
            "stt_result": full_text.strip(),
            "message": "Stride 최적화가 적용된 텍스트 변환을 성공했습니다."
        }
        
    except Exception as e:
        print(f"분석 중 에러 발생: {e}")
        return {"status": "error", "message": str(e)}
        
    finally:
        if os.path.exists(temp_file_path):
            os.remove(temp_file_path)

# m4a 입력 넣으려면 powershell에서 ffmpeg 다운