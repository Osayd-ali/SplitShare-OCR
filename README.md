Commands for localmachine usage

# commands for running on docker.
build: 
    docker build -t splitshare:latest .
run on localhost: 
    docker run -p 8080:8080 splitshare

# commands for running tests on docker.
build test on local machine : 
    docker build -f Dockerfile.test -t splitshare-test .
# run test on local machine : 
    docker run --rm splitshare-test

# run on location machine without tesseract libraries
mvn clean compile
mvn spring-boot:run