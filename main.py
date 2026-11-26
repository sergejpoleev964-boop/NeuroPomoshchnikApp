
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import requests
import os
import uuid

app = FastAPI()


class ChatRequest(BaseModel):
    message: str


@app.get("/")
def home():
    return {"status": "NeuroPomoshchnik server is running"}


@app.post("/chat")
def chat(data: ChatRequest):
    auth_key = "".join(
        os.environ.get("GIGACHAT_AUTH_KEY", "").split()
    )

    if not auth_key:
        raise HTTPException(
            status_code=500,
            detail="GIGACHAT_AUTH_KEY is not configured"
        )

    try:
        token_response = requests.post(
            "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
            headers={
                "Authorization": f"Basic {auth_key}",
                "RqUID": str(uuid.uuid4()),
                "Content-Type": "application/x-www-form-urlencoded",
                "Accept": "application/json"
            },
            data={"scope": "GIGACHAT_API_PERS"},
            timeout=30
        )

        token_response.raise_for_status()
        token = token_response.json()["access_token"]

        response = requests.post(
            "https://api.giga.chat/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            json={
                "model": "GigaChat",
                "messages": [
                    {
                        "role": "user",
                        "content": data.message
                    }
                ]
            },
            timeout=60
        )

        response.raise_for_status()

        answer = response.json()["choices"][0]["message"]["content"]
        return {"answer": answer}

    except requests.RequestException as error:
        raise HTTPException(
            status_code=502,
            detail=f"GigaChat connection error: {error}"
        )
    except (KeyError, ValueError):
        raise HTTPException(
            status_code=502,
            detail="Unexpected response from GigaChat"
        )
