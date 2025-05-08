import hazelcast

# Configurazione del client Hazelcast
client = hazelcast.HazelcastClient(
    cluster_name="dev",  # Nome del cluster
)

try:
    # Creazione o accesso a una mappa distribuita
    my_map = client.get_map("mappa-distribuita-1").blocking()

    # Memorizzazione di dati nella mappa
    my_map.put("chiave1", "valore1")
    my_map.put("chiave2", "valore2")

    # Recupero di un valore dalla mappa
    valore = my_map.get("chiave1")
    print(f"Valore recuperato per 'chiave1': {valore}")

finally:
    # Chiusura del client
    client.shutdown()