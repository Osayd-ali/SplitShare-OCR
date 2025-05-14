# SplitShare-OCR

SplitShare is a web application that revolutionizes how friends split expenses by using advanced OCR technology to scan receipts, automatically identify items, and intelligently allocate costs. Our solution eliminates the headache of manual calculations, prevents financial disputes, and makes group activities more enjoyable.

![Image Description](https://drive.google.com/uc?export=view&id=1JL1R89uPVPFx3C-5j8FsNruOaQ9MBVMp)

# Core Features
* **Intelligent Receipt Scanning:** Advanced OCR technology extracts itemized expenses from receipts
* **Smart Item Assignment:** Intuitive interface to assign items to specific group members
* **Automatic Calculations:** Fair distribution of tax, tip, and shared items
* **Instant Settlement:** Integration with popular payment platforms for immediate transfers
* **Group History:** Maintain records of past group activities and expenses

# Key Differentiators
* **Item-Level Precision:** Unlike competitors that only split total bills, we divide expenses at the item level
* **Receipt Scanning Technology:** Proprietary OCR algorithms specifically trained for receipt formats
* **Fairness Algorithm:** Proportional distribution of tax and tip based on individual spending
* **User Experience:** Designed for real-time use at the table, not after-the-fact reconciliation



# Commands for local-machine usage
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