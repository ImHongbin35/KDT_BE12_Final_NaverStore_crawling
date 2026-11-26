# 새로운 Python 프로젝트 폴더에 app.py 라는 이름으로 저장합니다.
# 실행 전 터미널에서 pip install flask sentence-transformers torch 설치가 필요합니다.

from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer, util

# -------------------------------------------------------------
# 1. 서버가 시작될 때 AI 모델을 미리 메모리에 로드해 둡니다. (시간이 좀 걸림)
#    (다국어 지원 모델을 사용하면 '애플워치' vs 'Apple Watch' 비교에 효과적)
print("Loading Sentence-Transformer model...")
model = SentenceTransformer('jhgan/ko-sroberta-multitask') 
print("Model loaded successfully.")
# -------------------------------------------------------------


app = Flask(__name__)

@app.route('/calculate-similarity', methods=['POST'])
def calculate_similarity():
    # 2. Java로부터 JSON 형식의 요청을 받습니다.
    #    예: { "text1": "애플워치", "text2": "최신형 Apple Watch 스트랩" }
    data = request.get_json()
    if not data or 'text1' not in data or 'text2' not in data:
        return jsonify({"error": "Invalid input"}), 400

    text1 = data['text1']
    text2 = data['text2']

    # 3. 모델을 사용해 각 텍스트를 숫자 벡터로 변환합니다.
    embedding1 = model.encode(text1, convert_to_tensor=True)
    embedding2 = model.encode(text2, convert_to_tensor=True)

    # 4. 두 벡터 간의 코사인 유사도를 '실시간으로 계산'합니다.
    #    결과는 0에서 1 사이의 점수로 나옵니다. (1에 가까울수록 유사)
    cosine_scores = util.cos_sim(embedding1, embedding2)
    similarity_score = cosine_scores.item()

    # 5. 계산된 점수를 JSON 형태로 Java에게 응답합니다.
    #    예: { "similarity": 0.87451... }
    return jsonify({"similarity": similarity_score})

@app.route('/health', methods=['GET'])
def health_check():
    """API 서버 상태 확인용 엔드포인트"""
    return jsonify({"status": "healthy", "message": "Sentence-Transformers API is running"})

if __name__ == '__main__':
    # 5000번 포트로 서버를 실행합니다.
    print("Starting Sentence-Transformers API server on port 5000...")
    app.run(port=5000, debug=True)