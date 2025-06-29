import logging
import time
import hazelcast


def wait_members_on(client: hazelcast.HazelcastClient, members_on: int, timeout: int = 60):
    """Wait for Hazelcast members to be online"""

    passed_time = 0
    while True:
        try:
            members = client.cluster_service.get_members()
        except Exception as e:
            continue
        if len(members) == members_on:
            break

        time.sleep(1)
        passed_time += 1
        if passed_time >= timeout:
            raise TimeoutError(f"Timeout: waited {timeout} seconds for {members_on} members to be online.")
