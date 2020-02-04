# Dragonglass-Hedera-Auction-Demo
Sample auction dapp shown in the "Building on Hedera Hashgraph with OpenCrowd &amp; DragonGlass" Hedera Virtual Meetup.

## Overview
This is a reference application for DApp developers to quickly able to setup an application running on Hedera Network.

## Description
There are three modules in the application
1. auction : This module provides a set of apis to create a new auction, start the auction and place bids ... etc. Hedera SDK is used to sign transactions
Please check the Readme file to be able to quickly run the application.
2. auctioneer-event : This module uses spring-cloud-aws-messaging to subscribe to DragonGlass events and spring-boot-websocket to pass the messages from DragonGlass queue to an open websocket 
3. auctioneer-frontend : This module is built on react version 16.12 and connects to DragonGlass APIs and subscribe to websocket queue to get events and reflect the changes on the UI.

