import React, { useState, useEffect } from 'react';
import * as actions from '../actions';
import { connect, useDispatch } from 'react-redux';
import Header from './Header';
import BidContainer from './BidContainer';
import TableContainer from './TableContainer';
import { getBids } from '../reducers';
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
    const apiKey = '<your_api_key>';

   const handleTxnUpdate = (txn) => {
        allEvents.unshift(txn);
        setA(([].concat(allEvents)));
   }
    const fetchData = async () => {
        await connect();
    }

    const displayWinner = (data) => {
        setWinner(data);
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
                    console.log("current bid - ", response.amount);
                    highestAmount = response.amount;
                    lastBidder = response.account;
                    handleTxnUpdate(response);
                    dispatch({type: "HIGHESTBID",
                          payload: highestAmount});
                });
                stompClient.subscribe('/queue/auctionEnd', function({body}) {
                      let response  = JSON.parse(body);
                      console.log(response);
                      displayWinner(response);
                  });
            });
        }
    /* -------------------------------------------------------------- */

    return (
        <div className="main">
            <Header />
            <BidContainer transactions={a} winner={winner} highestAmount={highestAmount} />
            <TableContainer transactions={a}/>
        </div>
    )
}

const mapStateToProps = (state) => ({
    transactions: getTransactions(state),
    users: getUsers(state)
});

export default connect(mapStateToProps, actions)(MainContainer);
