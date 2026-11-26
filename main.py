from fastapi import FastAPI
from pydantic import BaseModel
import requests
import os
import uuid

app = FastAPI()

class ChatRequest(BaseModel):
    message: str

@app.post("/chat")
def chat(data: ChatRequest):
    auth_key = os.environ["GIGACHAT_AUTH_KEY"]

    token_response = requests.post(
        "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
        headers={
            "Authorization": f"Basic {auth_key}",
            "RqUID": str(uuid.uuid4()),
            "Content-Type": "application/x-www-form-urlencoded"
        },
        data={"scope": "GIGACHAT_API_PERS"},
        verify="/etc/ssl/certs/ca-certificates.crt",
        timeout=30
    )

    token_response.raise_for_status()
    token = token_response.json()["access_token"]

    response = requests.post(
        "https://gigachat.devices.sberbank.ru/api/v1/chat/completions",
        headers={"Authorization": f"Bearer {token}"},
        json={
            "model": "GigaChat",
            "messages": [{"role": "user", "content": data.message}]
        },
        timeout=60
    )

    response.raise_for_status()
    answer = response.json()["choices"][0]["message"]["content"]
    return {"answer": answer}
