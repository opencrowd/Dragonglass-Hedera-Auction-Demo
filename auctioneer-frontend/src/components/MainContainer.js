import React, { useState, useEffect } from 'react';
import * as actions from '../actions';
import { connect, useDispatch } from 'react-redux';
import Header from './Header';
import BidContainer from './BidContainer';
import TableContainer from './TableContainer';
import { getTransactions } from '../reducers';
import { getUsers } from '../reducers';

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

    const dispatch = useDispatch();
    const apiKey = <your-api-key>;

   const handleTxnUpdate = (txn) => {
        allEvents.unshift(txn);
        setA(([].concat(allEvents)));
   }
    const fetchData = async () => {
        await props.fetchEvents(apiKey);
        await props.fetchUsers();
        await connect();
    }

    const displayWinner = (data) => {
        setWinner(data.inputValues);
    }

    let highestAmount = 0;
    let lastBidder = '';
    /* ---- CONNECTION TO THE WEBSOCKET ---- */
        const connect = () => {
            const socket = new SockJS('http://localhost:8080/auctioneer');
            const stompClient = Stomp.over(socket);
            stompClient.connect({}, (frame) => {
                stompClient.subscribe('/queue/bid', ({body}) => {
                    let item = {};
                    let response  = JSON.parse(body);
                    item["consensusTime"] = response.transactionID.validStartDate;
                    item["account"] = response.inputValues[0];
                    item["amount"] = response.inputValues[1];
                    if(response.inputValues[1] > highestAmount && response.inputValues[0] !== lastBidder){
                      highestAmount = response.inputValues[1];
                      lastBidder = response.inputValues[0];
                      handleTxnUpdate(item);
                      dispatch({type: "HIGHESTBID",
                        		payload: item.amount});
                        		}
                });
                stompClient.subscribe('/queue/auctionEnd', function({body}) {
                      let response  = JSON.parse(body);
                      displayWinner(response);
                  });
            });
        }
    /* -------------------------------------------------------------- */

    return (
        <div className="main">
            <Header />
            <BidContainer transactions={a} winner={winner} highestAmount={highestAmount} />
            <TableContainer userMap={userMap} transactions={a}/>
        </div>
    )
}

const mapStateToProps = (state) => ({
    transactions: getTransactions(state),
    users: getUsers(state)
});

export default connect(mapStateToProps, actions)(MainContainer);
