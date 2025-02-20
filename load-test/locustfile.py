from locust import task, FastHttpUser, between
import logging
import random

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
user_id_seq = 1
schedule_range = 10

class ReservationTest(FastHttpUser):
    connection_timeout = 10.0
    network_timeout = 10.0

    @classmethod
    def get_next_user_id(cls):
        """ 다음 User ID 반환 및 증가 """
        global user_id_seq
        user_id = user_id_seq
        user_id_seq += 1
        return user_id

    @task
    def make_reservation(self):
        busScheduleId = random.randint(1, schedule_range)
        self.user_id = self.get_next_user_id()
        headers = {
            "Content-Type": "application/json"
        }

        response = self.client.post(url="/api/reservation", headers=headers, json={
            "userId": self.user_id,
            "busScheduleId": busScheduleId
        })

        if response.status_code == 200:
            logger.info(f"✅ 예약 성공 - User ID: {self.user_id}")

    @task
    def confirm_reservation(self):
        busScheduleId = random.randint(1, schedule_range)
        self.user_id = user_id_seq
        headers = {
            "Content-Type": "application/json"
        }

        response = self.client.post(url="/api/reservation/confirm", headers=headers, json={
            "userId": self.user_id,
            "busScheduleId": busScheduleId
        })

        if response.status_code == 201:
            logger.info(f"✅ 예약 성공 - User ID: {self.user_id}, busScheduleId: {busScheduleId}")