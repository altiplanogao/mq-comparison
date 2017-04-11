docker run --name='activemq' -it --rm -P -p 61616:61616 -p 61613:61613 webcenter/activemq:5.14.3
service port: 61616
docker exec -it activemq bash




docker run -d --hostname my-rabbit -p 5672:5672 --name some-rabbit rabbitmq:3
service port: 5672
docker exec -it my-rabbit bash

#rabbitmqctl list_bindings
#rabbitmqctl list_exchanges




start a redis:
$ docker run --name some-redis -d redis
connect to it from an application
$ docker run --name some-app --link some-redis:redis -d application-that-uses-redis
... or via redis-cli
$ docker run -it --link some-redis:redis --rm redis redis-cli -h redis -p 6379