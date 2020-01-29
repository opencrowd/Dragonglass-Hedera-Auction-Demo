import React from 'react';

export const BidPaginator = ({ page, nextPage, prevPage, toBeginning, toEnd }) => {

    const nextRight = () => {
        (page.firstRow + page.rowsPerPage < page.totalRows) && nextPage();
    }

    const nextLeft = () => {
        page.firstRow > 1 && page.totalRows > 2 && prevPage();
    }

    const farLeft = () => {
        toBeginning();
    }

    const farRight = () => {
        page.rowsPerPage < page.totalRows && toEnd();
    }

    let lastDisplayedRow;
    if (parseInt(page.firstRow) + parseInt(page.rowsPerPage) <= page.totalRows) {
        lastDisplayedRow = `- ${parseInt(page.firstRow) + parseInt(page.rowsPerPage)}`
    } else if (parseInt(page.firstRow) + 1 === (parseInt(page.totalRows))) {
        lastDisplayedRow = ''
    } else if (parseInt(page.firstRow) + parseInt(page.rowsPerPage) > page.totalRows) {
        lastDisplayedRow = `- ${parseInt(page.totalRows)}`
    }

    return (
        <div className="bid-paginator"> 
            <div style={{marginRight: "10px"}}>Showing bids</div>
            <div style={{cursor: "pointer"}}><i className="icon angle double left" onClick={farLeft} /></div>
            <div style={{marginRight: "5px", cursor: "pointer"}}><i className="icon angle left" onClick={nextLeft} /></div>
            <div>{page.totalRows > 0 ? page.firstRow + 1 : 0} {lastDisplayedRow}</div>
            <div style={{marginLeft: "5px", cursor: "pointer"}}><i className="icon angle right" onClick={nextRight} /></div>
            <div style={{cursor: "pointer"}}><i className="icon angle double right" onClick={farRight} /></div>
            <div style={{marginLeft: "8px"}}>out of &nbsp;{page.totalRows}</div>
        </div>
    )
}