import logging
import subprocess
from time import sleep

from .client import create_client

from .utils import wait_members_on

from .tests.test import Test
from .tests.test_failover import FailoverTest
from .tests.test_map import MapTest
from .tests.test_map_scaling import MapScalingTest


single_node_tests = {
    "test_info": "single_node_tests",
    "tests": [
        MapTest,
    ]
}

two_nodes_tests = {
    "test_info": "two_nodes_tests",
    "tests": [
        MapTest,
        MapScalingTest,
        FailoverTest,
    ]
}

three_nodes_tests = {
    "test_info": "three_nodes_tests",
    "tests": [
        MapTest,
        MapScalingTest,
        FailoverTest,
    ]
}

def run_tests(tests_to_run: int = 0):
    """Main function to run Hazelcast tests with Docker Compose"""
    logger = logging.getLogger(__name__)
    logger.info("Starting tests...")
    # Import and run each test module
    if tests_to_run != 0:
        if tests_to_run == 1:
            tests_to_run = [single_node_tests]
        elif tests_to_run == 2:
            tests_to_run = [two_nodes_tests]
        elif tests_to_run == 3:
            tests_to_run = [three_nodes_tests]
        else:
            logger.error("Invalid test configuration. Please choose between 1 to 4.")
            return
    else:
        tests_to_run = [
            single_node_tests,
            two_nodes_tests,
            three_nodes_tests,
        ]

    # Check if Docker is available
    try:
        subprocess.run(["docker", "compose", "version"], check=True)
        subprocess.run(["docker", "compose", "down"], check=True)
    except subprocess.CalledProcessError:
        logger.error("Docker Compose is not available. Please install and activate Docker Compose to run the tests.")
        return
    

    try:
        subprocess.run(["docker", "compose", "down"], check=True)
        for test_case in tests_to_run:
            test_info = test_case["test_info"]
            if test_info == "single_node_tests":
                logger.info("Running single node tests...")
                subprocess.run(["docker", "compose", "up", "hazelcast-node1", "-d"], check=True)
                size = 1
            elif test_info == "two_nodes_tests":
                logger.info("Running two nodes tests...")
                subprocess.run(["docker", "compose", "up", "hazelcast-node1", "hazelcast-node2", "-d"], check=True)
                size = 2
            elif test_info == "three_nodes_tests":
                logger.info("Running three nodes tests...")
                subprocess.run(["docker", "compose", "up", "hazelcast-node1", "hazelcast-node2", "hazelcast-node3", "-d"], check=True)
                size = 3
            else:
                logger.error("Unknown test case configuration.")
                continue
            
            client = create_client()
            wait_members_on(client, members_on=size)
            client.shutdown()

            for test in test_case["tests"]:
                # Create an instance of the test class
                test_instance: Test = test(test_info)
                
                # Setup the test
                logger.info(f"Setting up test: {test}")
                test_instance.setup()
                
                # Run the test
                logger.info(f"Running test: {test}")
                if not test_instance.run_test():
                    logger.error(f"Test {test} failed.")

                # Teardown the test
                logger.info(f"Tearing down test: {test}")
                test_instance.teardown()
                logger.info(f"Test {test} completed successfully.")

            subprocess.run(["docker", "compose", "down"], check=True)
            sleep(10)
    except Exception as e:
        logger.error(f"An error occurred while running tests: {e}")
    finally:
        subprocess.run(["docker", "compose", "down"], check=True)

    logger.info("All tests completed.")
