import hazelcast
import logging

import hazelcast.config

def create_client(
        address_list: list[str] = None,
        cluster_name: str = "dev",
) -> hazelcast.HazelcastClient:
    """Create a Hazelcast client with retry configuration"""
    
    # Configure logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    # Client configuration with retry
    config = hazelcast.config.Config()

    config.cluster_name = cluster_name
    if address_list:
        config.cluster_members = address_list

    client = hazelcast.HazelcastClient(config)

    logger.info("Hazelcast client initialized")
    return client
