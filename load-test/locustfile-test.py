from locust import task, FastHttpUser, TaskSet, between, User
import logging
import random
import time

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
user_id_seq = 1
max_user_num = 2000
scheduleNum = 15

class SequencialApiCalls(TaskSet):
    user_id_seq = 1

    def __init__(self, parent: User):
        super().__init__(parent)
        self.user_id = 1

    @task
    def test_reservation(self):
        global user_id_seq
        self.user_id = self.get_next_user_id()
        headers = {
            "Content-Type": "application/json"
        }
        user_id = random.randint(1, max_user_num)
        busScheduleId = random.randint(1, scheduleNum)

        response = self.client.post(url="/api/reservation", headers=headers, json={
            "userId": user_id,
            "busScheduleId": busScheduleId,
        })

        if response.status_code != 200:
            return

        time.sleep(random.randint(1, 5))

        response = self.client.post(url="/api/reservation/confirm", headers=headers, json={
            "userId": user_id,
            "busScheduleId": busScheduleId,
        })

        if response.status_code == 201:
            logger.info(f"✅ 티켓 구매 성공 - User ID: {self.user_id}, busScheduleId: {busScheduleId}")

        # if response.status_code == 201:
        #     logger.info(f"✅ 예매 성공 - User ID: {self.user_id}, busScheduleId: {busScheduleId}")

    # @task
    # def step_2(self):
    #     global user_id_seq
    #     self.user_id = user_id_seq
    #     headers = {
    #         "Content-Type": "application/json"
    #     }
    #     user_id = random.randint(1, 1000)
    #     busScheduleId = random.randint(1, scheduleNum)
    #
    #     response = self.client.post(url="/api/reservation/confirm", headers=headers, json={
    #         "userId": user_id,
    #         "busScheduleId": busScheduleId,
    #     })
    #
    #     if response.status_code == 201:
    #         logger.info(f"✅ 티켓 구매 성공 - User ID: {self.user_id}, busScheduleId: {busScheduleId}")

    @classmethod
    def get_next_user_id(cls):
        """ 다음 User ID 반환 및 증가 """
        global user_id_seq
        user_id = user_id_seq
        user_id_seq += 1
        return user_id

class ReservationTest(FastHttpUser):
    connection_timeout = 10.0
    network_timeout = 10.0
    tasks = [SequencialApiCalls]

