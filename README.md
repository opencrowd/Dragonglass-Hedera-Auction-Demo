# Dragonglass-Hedera-Auction-Demo 
This repo contain the code for Sample auction dApp presented in the [Hedera Virtual Meetup: Building on Hedera Hashgraph with OpenCrowd &amp; DragonGlass](https://youtu.be/oA6riJv3RRA).

## Overview 
This is a reference application for DApp developers to quickly able to setup an application running on Hedera networks.

## Description 
There are three modules in the application   
1. auction : This module, built with spring boot, is the backend for the demo and provides a set of rest APIs to create a new auction, start the auction and place bids. Hedera SDK is used to sign and submit smart contract transactions to Hedera networks. Please check the Readme file for the module to quickly setup and run the application.
2. auctioneer-event : This module uses spring-cloud-aws-messaging to subscribe to DragonGlass events and spring-boot-websocket to pass the messages from DragonGlass queue to an open websocket.
3. auctioneer-frontend : This module is built on react version 16.12 and connects to DragonGlass APIs and subscribes to websocket queue to get events and renders the data on the UI.

## Enhancements & Issues
For enhancements and issues, please add a request/issue at [dragonglass-auction-demo](https://github.com/opencrowd/Dragonglass-Hedera-Auction-Demo/projects/1) project.
