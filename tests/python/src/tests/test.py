
from abc import ABC, abstractmethod
from typing import Optional
from hazelcast import HazelcastClient

from ..client import create_client
from ..reporting import TestReport


class Test(ABC):
    """Base class for all tests, providing common setup and teardown methods."""

    report: TestReport
    """Test report instance to log results."""

    client: Optional[HazelcastClient]
    """Hazelcast client instance for interacting with the cluster."""

    def __init__(self, test_name, test_info):
        """Initialize the test with a name and create a report instance."""
        self.report = TestReport(test_name, test_info)
        self.client = None
    
    def setup(self):
        """Connect to the Hazelcast cluster"""
        self.client = create_client()

    @abstractmethod
    def run_test(self) -> bool:
        """Run the specific test logic"""
        pass

    def custom_teardown(self):
        """Custom teardown logic for specific tests"""
        pass

    def teardown(self):
        """Clean up resources"""

        objs = self.client.get_distributed_objects()
        # Ensure all distributed objects are cleaned up
        for obj in objs:
            try:
                obj.destroy()
            except Exception as e:
                print(f"Error destroying object {obj}: {e}")

        self.custom_teardown()
        if self.client:
            self.client.shutdown()
        
        # Generate report
        self.report.save_to_csv()
