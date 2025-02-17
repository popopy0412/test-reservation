from locust import task, FastHttpUser, between
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class HelloWorld(FastHttpUser):
    connection_timeout = 10.0
    network_timeout = 10.0
    user_id_seq = 1  # 클래스 변수 (모든 인스턴스가 공유)

    @classmethod
    def get_next_user_id(cls):
        """ 다음 User ID 반환 및 증가 """
        user_id = cls.user_id_seq
        cls.user_id_seq += 1
        return user_id

    @task
    def make_reservation(self):
        global user_id_seq
        self.user_id = self.get_next_user_id()
        headers = {
            "Content-Type": "application/json"
        }

        response = self.client.post(url="/api/reservation", headers=headers, json={
            "userId": self.user_id,
            "busScheduleId": 1
        })

        if response.status_code == 200:
            logger.info(f"✅ 예약 성공 - User ID: {self.user_id}")
