import React, { useState } from 'react';
import * as actions from '../actions';
import { connect } from 'react-redux';
import { getBid } from '../reducers';

const BidContainer = ({ transactions=[], winner=[], placeBid, highestAmount, bid }) => {

    const [inputValue, setInputValue] = useState('');

    const handleChange = (e) => {
        setInputValue(e.target.value);
    }

    const userData = {"0.0.29113": "Alice", "0.0.29114": "Bob", "0.0.29115": "Carol"};

    const handleClick = () => {
        placeBid(inputValue);
        setInputValue('');
    }

    return (
        <div className="bid-panel">
            <img className="headphones" alt="headphones" src={require("../img/desktop-hd-bitmap@2x.png")} />

            <div className="bid-info">
                <div className="bose">
                    Bose 995c Wireless Bluetooth Headphones
                </div>

                {!winner || winner.length === 0 ?
                <div className="bid-text">
                    <span className="current-bid">Current Bid: </span>
                    <span className="bid-price"><span className="tiny">t</span>ℏ {bid} </span>
                </div> :
                <div className="bid-text">
                    <span className="current-bid">Winner: </span>
                    <span className="bid-price">{userData[winner[0]]} </span><br/>
                    <span className="current-bid">Winning Bid: </span>
                    <span className="bid-price"><span className="tiny">t</span>ℏ {winner[1]} </span>
                </div>
                }

                {!winner || winner.length === 0 &&
                <div className="input-row">
                    <input className="bid-input" placeholder="Enter your bid" value={inputValue} onChange={e => handleChange(e)} />
                    <button className="bid-button" onClick={handleClick} disabled={inputValue===''} >Place Bid</button>
                </div>
                }
            </div>
            
        </div>
    )
}
const mapStateToProps = (state) => ({
    bid: getBid(state)
});

export default connect(mapStateToProps, actions)(BidContainer);
