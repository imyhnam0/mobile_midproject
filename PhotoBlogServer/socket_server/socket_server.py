import socket
from datetime import datetime

HOST = '0.0.0.0'   # 모든 IP에서 접속 허용
PORT = 8000        # 서버 포트

# 1️⃣ TCP 소켓 생성
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((HOST, PORT))
server_socket.listen(1)

print(f"[+] Socket Server listening on {HOST}:{PORT}")

# 2️⃣ 요청 대기 루프
while True:
    client_socket, addr = server_socket.accept()
    print(f"[+] Connected by {addr}")

    # 3️⃣ 클라이언트로부터 요청 수신
    request_data = client_socket.recv(4096)

    # 4️⃣ 파일로 저장 (시간 기반 파일명)
    filename = datetime.now().strftime("%Y-%m-%d-%H-%M-%S") + ".bin"
    with open(filename, "wb") as f:
        f.write(request_data)
    print(f"[+] Request saved to {filename}")

    # 5️⃣ 간단한 응답 전송
    response = b"HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nReceived"
    client_socket.sendall(response)
    client_socket.close()
