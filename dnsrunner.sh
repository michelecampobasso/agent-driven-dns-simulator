#!/bin/bash 

java -cp libs/jade.jar:bin jade.Boot -container -agents "210.210.210.210:dns.client.agents.ClientAgent(post);222.222.222.222:dns.server.agents.TopLevelDomainServerAgent(post);223.223.223.223:dns.server.agents.TopLevelDomainServerAgent(post);244.244.244.244:dns.server.agents.DNSServerAgent(post)" -host 192.168.56.1 -port 1099 &