import argparse
import os

if __name__ == "__main__":
    from src.run_tests import run_tests

    # Set up argument parser
    parser = argparse.ArgumentParser(description='Run tests with configurable report directory.')
    parser.add_argument('--report-dir', dest='report_dir', type=str, default=None, help='Directory where reports will be stored')
    
    parser.add_argument('--tests', dest='tests_to_run', type=int, choices=[0, 1, 2, 3, 4, 5], default=0, help='Specify which tests to run: 0 for all, 1 for single node, 2 for two nodes, 3 for three nodes, ecc.')
    
    # Parse arguments
    args = parser.parse_args()
    
    # Set environment variable with a default if not specified
    if args.report_dir:
        os.environ['REPORT_DIR'] = args.report_dir
    else:
        # Set a default value for REPORT_DIR
        default_report_dir = os.path.join(os.getcwd(), 'reports')
        os.environ['REPORT_DIR'] = default_report_dir
    
    # Run the tests
    run_tests(args.tests_to_run)
