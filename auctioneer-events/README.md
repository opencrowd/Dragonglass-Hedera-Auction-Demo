# Backend for Dragonglass Auction Demo

## Event Listener Overview
This project is the subscribe to DragonGlass queues created using [Realtime Live Subscription](https://app.dragonglass.me/hedera/subscriptionview).  
Once the message is received, the message is then pushed to a websocket

## Description
This application uses spring-cloud-aws-messaging to subscribe to DragonGlass Events. The application subscribes to 2 event queues, 
one is for getting latest bid(HighestBidIncreased event emitted by contract call) and for auctionEnd(AuctionEnded event emitted by contract call).

On receiving the message, it is passed to the Spring-boot-websocket STOMP broker.

## Quickstart
Run EventMain class

### Sample Output
#### Bid Event
```json
{
  "functionName": "HighestBidIncreased",
  "functionType": "event",
  "inputNames": [
    "bidder",
    "amount"
  ],
  "inputTypes": [
    "address",
    "uint256"
  ],
  "inputValues": [
    "0.0.156807",
    "1008"
  ],
  "transactionID": {
    "accountID": {
      "num": 156807,
      "shardNum": 0,
      "realmNum": 0
    },
    "validStartDate": "2020-02-04T16:29:16.681829100Z"
  }
}
```
#### Winner Event
```json
{
  "functionName": "AuctionEnded",
  "functionType": "event",
  "inputNames": [
    "winner",
    "amount"
  ],
  "inputTypes": [
    "address",
    "uint256"
  ],
  "inputValues": [
    "0.0.156807",
    "1008"
  ],
  "transactionID": {
    "accountID": {
      "num": 155274,
      "shardNum": 0,
      "realmNum": 0
    },
    "validStartDate": "2020-02-04T16:29:20.328925Z"
  }
}
```

### Requirements

- JDK 11
- [Hedera Java SDK](https://github.com/hashgraph/hedera-sdk-java.git)

### Installation
```bash
git clone https://github.com/opencrowd/Dragonglass-Hedera-Auction-Demo.git
cd Dragonglass-Hedera-Auction-Demo/auctioneer-event
nano src/main/resources/application.yml # Insert bid queuee
./mvnw clean install
```
### Yaml Profile Configuration
```text
dragonglass:
  queue:
    bid: <bid-queue from DG>
    auctionEnd: <auction-End queue from DG>
```
### Project structure
- ./README.md: this file
- ./pom.xml
- ./src/main/java: java source code
- ./src/main/resources: yml config file
