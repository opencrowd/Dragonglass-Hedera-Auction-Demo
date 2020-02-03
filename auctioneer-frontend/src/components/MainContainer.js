import React, { useState, useEffect } from 'react';
import * as actions from '../actions';
import { connect, useDispatch } from 'react-redux';
// import Websocket from 'react-websocket';
import Header from './Header';
import BidContainer from './BidContainer';
import TableContainer from './TableContainer';
import { getBids } from '../reducers';
import { getTransactions } from '../reducers';
import { getUsers } from '../reducers';
// import { getTime } from '../reducers';
// import SockJsClient from 'react-stomp';

import * as SockJS from 'sockjs-client';
import * as Stomp from 'stompjs';

let allEvents = [];

const MainContainer = (props) => {
    // eslint-disable-next-line react-hooks/exhaustive-deps

    useEffect(() => { fetchData() }, []);

    const [a, setA] = useState([]);
    const [userMap, setUserMap] = useState({});
    const [winner, setWinner] = useState([]);

    useEffect(() => { allEvents = allEvents.concat(props.transactions) }, [props.transactions]);
    useEffect(() => { props.users.length > 0 && setUserMap(props.users[0]) }, [props.users]);
    useEffect(() => { setConsensusStartInEpoch(Date.now()) }, [props.time]);

    // const contractId = '0.0.143796';
    // const contractId = '0.0. 143766';
    // const contractId = '0.0.143773';
    const dispatch = useDispatch();
    const apiKey = '<api-key>';     // This is a mainnet apiKey

   const handleTxnUpdate = (txn) => {
        allEvents.unshift(txn);
        setA(([].concat(allEvents)));
   }
    const fetchData = async () => {
        // await props.fetchTransactions(contractId, apiKey);
        await props.fetchEvents(apiKey);
        await props.fetchUsers();
        //await props.resetAuction();
        await connect();
    }



    const [consensusStartInEpoch, setConsensusStartInEpoch] = useState(Date.now());


    const displayWinner = (data) => {
        setWinner(data.inputValues);
        console.log("THE WINNER IS: ", data.inputValues)
    }

   //let highestAmount = transactions && transactions[0] ? transactions[0].amount : 0;
    let highestAmount = 0;
    /* ---- NEW FUNCTION ATTEMPTING TO CONNECT TO THE WEBSOCKET ---- */
        const connect = () => {
            const socket = new SockJS('http://localhost:8080/auctioneer');
            const stompClient = Stomp.over(socket);
            stompClient.connect({}, (frame) => {
                // setConnected(true);
                stompClient.subscribe('/queue/bid', ({body}) => {
                    let item = {};
                    let response  = JSON.parse(body);
                    item["consensusTime"] = response.transactionID.validStartDate;
                    item["account"] = response.inputValues[0];
                    item["amount"] = response.inputValues[1];
                    console.log("current bid - ", response.inputValues[1]);
                    highestAmount = response.inputValues[1];
                    handleTxnUpdate(item);
                    dispatch({type: "HIGHESTBID",
                          payload: item.amount});
                });
                stompClient.subscribe('/queue/auctionEnd', function({body}) {
                      let response  = JSON.parse(body);
                      // showMessageOutput(JSON.parse(messageOutput.body));
                      displayWinner(response);
                  });
            });
        }
        /* -------------------------------------------------------------- */


    return (
        <div className="main">

            <Header />
            <BidContainer /*bids={bids}*/ transactions={a} winner={winner} highestAmount={highestAmount} />
            <TableContainer userMap={userMap} transactions={a}/>
            
        </div>
    )
}

const mapStateToProps = (state) => ({
    bids: getBids(state),
    transactions: getTransactions(state),
    users: getUsers(state),
    // time: getTime(state)
});

export default connect(mapStateToProps, actions)(MainContainer);
