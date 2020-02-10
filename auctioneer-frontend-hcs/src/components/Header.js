import React from 'react';
import * as actions from '../actions';
import { connect } from 'react-redux';
import '../css/desktopHd.css'

const Header = ({ resetAuction, setTime }) => {

    const handleReset = () => {
        console.log("RESET CLICKED");
        resetAuction();
        // setTime();
    }

    return (
        <div className="header">
            {/*<div className="heading span1">A</div>*/}<div className="heading span2">DragonGlass Auction Demo DApp</div>
            <div className="spacer"></div>
            <img className="round-image" alt="" src={require("../img/Alice.png")} />
            <div className="bidder">Alice</div>
            <img className="arrow-down" alt="arrow-down" src={require("../img/desktop-hd-u21b3ud83cudf08-color-1@2x.png")} />
            <button className="reset" onClick={handleReset}><span style={{marginLeft: "-2.5px"}}>START</span></button>
        </div>
    )
}

export default connect(null, actions)(Header);
