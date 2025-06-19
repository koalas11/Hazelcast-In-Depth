import csv
import os
import time
import datetime

class TestReport:
    def __init__(self, test_name, test_info: str = "single node test"):
        """Initialize the test report with a name and test case"""
        self.test_name = test_name
        self.test_info = test_info
        self.results = []
        self.metrics = {}
    
    def add_result(self, test_case, status, details=""):
        """Add a test result to the report"""
        self.results.append({
            "timestamp": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "test_case": test_case,
            "status": status,
            "details": details
        })
    
    def add_metric(self, name, value):
        """Add a metric measurement to the report"""
        self.metrics[name] = value

    def save_to_csv(self):
        """Save test results and metrics to CSV file"""
        reports_dir = os.environ.get('REPORTS_DIR', 'reports')
        os.makedirs(reports_dir, exist_ok=True)
        
        # Save test results
        results_filename = os.path.join(reports_dir, self.test_info, f"{self.test_name}.csv")
        os.makedirs(os.path.dirname(results_filename), exist_ok=True)
        with open(results_filename, 'w', newline='') as csvfile:
            fieldnames = ['timestamp', 'test_case', 'status', 'details']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            
            if csvfile.tell() == 0:  # Write header only if file is empty
                writer.writeheader()
            for result in self.results:
                writer.writerow(result)
        
        # Save metrics to a separate file if any exist
        if self.metrics:
            metrics_filename = os.path.join(reports_dir, self.test_info, f"{self.test_name}_metrics.csv")
            with open(metrics_filename, 'w', newline='') as csvfile:
                fieldnames = ['metric_name', 'value']
                writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                
                if csvfile.tell() == 0:  # Write header only if file is empty
                    writer.writeheader()
                for name, value in self.metrics.items():
                    writer.writerow({'metric_name': name, 'value': value})
            
            print(f"Results saved to {results_filename}")
            print(f"Metrics saved to {metrics_filename}")
            return results_filename, metrics_filename
        
        print(f"Report saved to {results_filename}")
        return results_filename
