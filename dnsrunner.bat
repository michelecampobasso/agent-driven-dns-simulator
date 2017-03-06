break>added_hosts.txt
java -cp libs\jade.jar;bin jade.Boot -agents ^
RootServer:dns.server.agents.RootServerAgent;^
110.110.110.110:dns.client.agents.ClientAgent();^
123.123.123.123:dns.server.agents.TopLevelDomainServerAgent();^
123.123.123.124:dns.server.agents.TopLevelDomainServerAgent();^
133.133.133.133:dns.server.agents.DNSServerAgent();^
133.133.133.134:dns.server.agents.DNSServerAgent();^
144.144.144.143:dns.server.agents.DNSServerAgent();^
144.144.144.144:dns.server.agents.DNSServerAgent();^
210.210.210.210:dns.client.agents.ClientAgent();^
222.222.222.222:dns.server.agents.TopLevelDomainServerAgent();^
223.223.223.223:dns.server.agents.TopLevelDomainServerAgent();^
231.231.231.231:dns.server.agents.DNSServerAgent();^
231.231.231.232:dns.server.agents.DNSServerAgent();^
244.244.244.243:dns.server.agents.DNSServerAgent();^
244.244.244.244:dns.server.agents.DNSServerAgent();^
GOD:dns.theinternet.TheInternetAgent -local-host 192.168.56.1 -local-port 1099 -gui &