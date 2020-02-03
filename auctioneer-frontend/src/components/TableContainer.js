import React, { useState, useEffect } from 'react';
import { Table } from 'semantic-ui-react';
import { connect } from 'react-redux';
import { BidPaginator } from './BidPaginator';
import { getBids } from '../reducers';
import { getTransactions } from '../reducers';

const TableContainer = ({ transactions = [], userMap }) => {

    const [page, setPage] = useState({
		totalRows: transactions.length,
		rowsPerPage: 5,
		firstRow: 0
    });

    useEffect(() => {if (transactions && transactions.length > 0) setPage({ 
        ...page, 
        // eslint-disable-next-line react-hooks/exhaustive-deps
        totalRows: transactions.filter(el => el).length})}, [transactions])

	const nextPage = () => {
		setPage({ ...page, firstRow: parseInt(page.firstRow) + parseInt(page.rowsPerPage)});
    };
    
	const prevPage = () => {
        if (parseInt(page.firstRow) - parseInt(page.rowsPerPage) < 0) {
            setPage({ ...page, firstRow: 0});
        } else {
            setPage({ ...page, firstRow: parseInt(page.firstRow) - parseInt(page.rowsPerPage)});
        }
    };
    
	const toBeginning = () => {
		setPage({ ...page, firstRow: 0 })
    };
    
	const toEnd = () => {
		setPage({ ...page, firstRow: (page.totalRows - /*page.totalRows % */page.rowsPerPage) })
    };
    
    let transactionsToDisplay = transactions && transactions.slice(parseInt(page.firstRow), (parseInt(page.firstRow) + parseInt(page.rowsPerPage)));

    const rowsToDisplay = transactions && transactions.length > 0 && transactionsToDisplay && transactionsToDisplay.map(el => {
        return el && (
            <Table.Row key={Math.random()}>
                {el && userMap && <Table.Cell className="price" style={{textAlign: "left"}}>
                    {userMap[el["account"]]}
                </Table.Cell>}
                
                {el && <Table.Cell className="account" style={{textAlign: "center"}}>
                    {/* <a href={`https://testnet.dragonglass.me/hedera/transactions/${el.transactionID}`} target="_blank"> */}
                        {el.account}
                    {/* </a> */}
                </Table.Cell>}
                
                {el && <Table.Cell className="price" style={{textAlign: "center"}}>
                    {el.amount}
                </Table.Cell>}
                
                {el && <Table.Cell className="price" style={{textAlign: "right"}}>{el.consensusTime.slice(11, 19)}</Table.Cell>}
            </Table.Row>
        )
    });

    return (
        <div className="table-panel">
            <div className="bidding-history">Bidding History</div>

            <div className="table">
                <Table basic='very'>
                    <Table.Header>
                    <Table.Row>
                        <Table.HeaderCell>Name</Table.HeaderCell>
                        <Table.HeaderCell style={{textAlign: "center"}}>Bid Account</Table.HeaderCell>
                        <Table.HeaderCell style={{textAlign: "center"}}>Bid Amount</Table.HeaderCell>
                        <Table.HeaderCell style={{textAlign: "right"}}>Time</Table.HeaderCell>
                        {/* <Table.HeaderCell style={{textAlign: "center"}}>Success/Failure</Table.HeaderCell> */}
                    </Table.Row>
                    </Table.Header>

                    <Table.Body>
                        {rowsToDisplay}
                    </Table.Body>
                </Table>
                {transactions.length > 0 && <BidPaginator page={page} nextPage={nextPage} prevPage={prevPage} toBeginning={toBeginning} toEnd={toEnd} />}
            </div>
        </div>
    )
}

const mapStateToProps = (state) => ({
    bids: getBids(state),
    //transactions: getTransactions(state)
});

export default connect(mapStateToProps)(TableContainer);