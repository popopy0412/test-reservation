from locust import task, FastHttpUser, TaskSet, between, User
import logging
import random

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
user_id_seq = 1

class SequencialApiCalls(TaskSet):
    user_id_seq = 1

    def __init__(self, parent: User):
        super().__init__(parent)
        self.user_id = 1

    @task
    def step_1(self):
        global user_id_seq
        self.user_id = self.get_next_user_id()
        headers = {
            "Content-Type": "application/json"
        }
        user_id = random.randint(1, 1000)
        busScheduleId = random.randint(1,3)

        response = self.client.post(url="/api/reservation", headers=headers, json={
            "userId": user_id,
            "busScheduleId": busScheduleId,
        })

        if response.status_code == 200:
            logger.info(f"✅ 임시 예매 성공 - User ID: {self.user_id}")

    @task
    def step_2(self):
        global user_id_seq
        self.user_id = user_id_seq
        headers = {
            "Content-Type": "application/json"
        }
        user_id = random.randint(1, 1000)
        busScheduleId = random.randint(1,3)

        response = self.client.post(url="/api/reservation/confirm", headers=headers, json={
            "userId": user_id,
            "busScheduleId": busScheduleId,
        })

        if response.status_code == 200:
            logger.info(f"✅ 티켓 구매 성공 - User ID: {self.user_id}")

    @classmethod
    def get_next_user_id(cls):
        """ 다음 User ID 반환 및 증가 """
        global user_id_seq
        user_id = user_id_seq
        user_id_seq += 1
        return user_id

class HelloWorld(FastHttpUser):
    connection_timeout = 10.0
    network_timeout = 10.0
    wait_time = lambda self: 2
    tasks = [SequencialApiCalls]

