import json
import os
from pydub import AudioSegment
import imageio_ffmpeg

# ----------------------------------------------------
# 1. 환경 및 경로 세팅 (C드라이브 도마 위로 세팅!)
# ----------------------------------------------------
AudioSegment.converter = imageio_ffmpeg.get_ffmpeg_exe()

INPUT_DIR = r"C:\Users\최재현\Desktop\AI_Whisper_Tuning"
# 타겟을 다시 C드라이브 바탕화면으로 변경!
OUTPUT_BASE_DIR = r"C:\Users\최재현\Desktop\Tuning_output" 

LABELING_DIR = os.path.join(INPUT_DIR, "labeling")
RESOURCE_DIR = os.path.join(INPUT_DIR, "resource")
OUTPUT_DIR = os.path.join(OUTPUT_BASE_DIR, "dataset_processed")

os.makedirs(OUTPUT_DIR, exist_ok=True)

# ----------------------------------------------------
# 2. 핵심 썰기 함수 (SSD니까 0.1초 휴식 제거! 풀악셀!)
# ----------------------------------------------------
def process_single_file(json_path, audio_path, output_dir, base_filename):
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    audio = AudioSegment.from_file(audio_path)
    utterances = data['utterance']
    
    current_chunk_text = ""
    chunk_start_time = float(utterances[0]['start'])
    last_end_time = chunk_start_time
    chunk_id = 1
    
    for u in utterances:
        u_start = float(u['start'])
        u_end = float(u['end'])
        
        if (u_end - chunk_start_time) > 28.0 and current_chunk_text != "":
            audio_chunk = audio[int(chunk_start_time * 1000):int(last_end_time * 1000)]
            
            # SSD는 튼튼하니까 딜레이 없이 다이렉트 저장
            audio_chunk.export(os.path.join(output_dir, f"{base_filename}_chunk_{chunk_id}.wav"), format="wav")
            with open(os.path.join(output_dir, f"{base_filename}_chunk_{chunk_id}.txt"), "w", encoding="utf-8") as tf:
                tf.write(current_chunk_text.strip())
                
            chunk_start_time = u_start
            current_chunk_text = ""
            chunk_id += 1

        current_chunk_text += u['form'] + " "
        last_end_time = u_end

    if current_chunk_text != "":
        audio_chunk = audio[int(chunk_start_time * 1000):int(last_end_time * 1000)]
        audio_chunk.export(os.path.join(output_dir, f"{base_filename}_chunk_{chunk_id}.wav"), format="wav")
        with open(os.path.join(output_dir, f"{base_filename}_chunk_{chunk_id}.txt"), "w", encoding="utf-8") as tf:
            tf.write(current_chunk_text.strip())

# ----------------------------------------------------
# 3. 전체 데이터셋 스킵 & 런 루프
# ----------------------------------------------------
print(f"🚀 [C드라이브 풀악셀] 전체 데이터셋 전처리 시작...\n")
processed_count = 0
skipped_count = 0
error_count = 0

for root, dirs, files in os.walk(LABELING_DIR):
    for file in files:
        if file.endswith(".json"):
            json_path = os.path.join(root, file)
            
            relative_path = os.path.relpath(root, LABELING_DIR)
            wav_dir = os.path.join(RESOURCE_DIR, relative_path)
            wav_filename = file.replace(".json", ".wav")
            wav_path = os.path.join(wav_dir, wav_filename)
            
            if os.path.exists(wav_path):
                base_name = file.replace(".json", "")
                
                # C드라이브에 이미 썰린게 있으면 스킵
                if os.path.exists(os.path.join(OUTPUT_DIR, f"{base_name}_chunk_1.wav")):
                    skipped_count += 1
                    if skipped_count % 500 == 0:
                        print(f"⏭️ {skipped_count}개 파일 스킵 완료...")
                    continue
                
                print(f"🔪 [{processed_count + 1}번째] {base_name} 썰기 중...")
                
                try:
                    process_single_file(json_path, wav_path, OUTPUT_DIR, base_name)
                    processed_count += 1
                except Exception as e:
                    print(f"  🗑️ [포기] {base_name} 실패. 쿨하게 버림 (사유: {e})")
                    error_count += 1

print(f"\n🎉 [최종 완료] 성공: {processed_count}개 | 스킵: {skipped_count}개 | 에러로 버림: {error_count}개")