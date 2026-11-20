# Command Cheat Sheet

Most-used commands for this project. Run them from the repo root unless stated otherwise.

## Java / SDKMAN (install JDK 25)
```bash
curl -s "https://get.sdkman.io" | bash         # install SDKMAN (once), then open a new terminal
sdk list java | grep -E " 25\."                # see available JDK 25 builds
sdk install java 25-tem                        # install Temurin 25 (use the exact id from the list)
sdk use java 25-tem                            # use it in this terminal only
sdk default java 25-tem                        # make it the default
java -version                                  # verify
echo $JAVA_HOME                                # Maven uses this
```

## Maven
```bash
mvn -v                                         # Maven + Java version in use
mvn clean                                      # delete target/ folders
mvn compile                                    # compile only
mvn test                                       # run unit tests
mvn clean package                              # build jars (runs tests)
mvn clean package -DskipTests                  # build jars, skip tests
mvn clean install                              # build and put jars in local ~/.m2
mvn dependency:tree                            # show all dependencies
mvn versions:display-dependency-updates        # show newer dependency versions
mvn help:effective-pom                         # POM after inheritance
```

### Maven for one module of the multi-module project
```bash
mvn -pl product-service clean package          # build only product-service
mvn -pl product-service -am clean package      # also build the modules it depends on
mvn -pl product-service spring-boot:run        # run product-service
mvn -pl product-service test -Dtest=ProductServiceTest        # run one test class
mvn -pl product-service test -Dtest=ProductServiceTest#create # run one test method
```

### Run a Spring Boot service
```bash
mvn -pl product-service spring-boot:run -Dspring-boot.run.profiles=dev     # with a profile
java -jar product-service/target/product-service-1.0.0-SNAPSHOT.jar        # run the built jar
java -jar app.jar --server.port=9090                                       # override a property
```

## Docker
```bash
docker ps                                      # running containers
docker ps -a                                   # all containers
docker images                                  # local images
docker logs -f <container>                     # follow logs
docker exec -it <container> bash               # shell inside a container
docker stop <container>                        # stop
docker rm <container>                          # remove a stopped container
docker rmi <image>                             # remove an image
docker system prune                            # remove unused containers, networks, images
docker volume ls                               # list volumes
docker volume rm <volume>                      # delete a volume (data is lost)
docker build -t shopsphere/product-service .   # build an image from a Dockerfile
docker run -p 8081:8081 shopsphere/product-service   # run an image
```

## Docker Compose
```bash
docker compose up -d                           # start everything in background
docker compose up -d --build                   # rebuild images, then start
docker compose ps                              # status (wait for "healthy")
docker compose logs -f mysql                   # follow one service's logs
docker compose stop                            # stop, keep containers
docker compose down                            # stop and remove containers
docker compose down -v                         # also delete volumes (wipes DB, re-runs init.sql)
docker compose restart mysql                   # restart one service
```

## MySQL (inside our Docker container)
```bash
docker exec -it shopsphere-mysql mysql -uroot -proot                       # open MySQL shell as root
docker exec -it shopsphere-mysql mysql -uroot -proot -e "SHOW DATABASES;"  # one-off query
docker exec -it shopsphere-mysql mysql -uproduct_user -pproduct_pass product_db   # as service user
```
Useful inside the shell:
```sql
SHOW DATABASES;
USE product_db;
SHOW TABLES;
DESCRIBE products;
SELECT * FROM products;
```
Connection details from your machine: host `localhost`, port **3307**.

## Testing the APIs with curl
```bash
curl -i http://localhost:8081/actuator/health                                  # health check
curl -i http://localhost:8081/api/products                                     # GET list
curl -i http://localhost:8082/api/orders                                       # GET orders (order-service)
curl -i -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":55000,"stock":10}'                              # POST JSON
curl -i -X DELETE http://localhost:8081/api/products/1                         # DELETE
```

## Ports and who is using them
```bash
ss -ltnp | grep 8081                           # who uses port 8081
kill <PID>                                     # stop that process
```

## Git (our lesson routine)
```bash
git status
git add -A
git commit -m "Lesson X.Y: short description"
git push
git --no-pager log --oneline                   # history, one line each
```
