import React from 'react';
import { BrowserRouter as Router } from 'react-router-dom';
import { Provider } from 'react-redux';
import App from './App';

const Root = ({ store }) => (
	<Provider store={store}>
		<Router basename="/">
			<App />
		</Router>
	</Provider>
);

export default Root;