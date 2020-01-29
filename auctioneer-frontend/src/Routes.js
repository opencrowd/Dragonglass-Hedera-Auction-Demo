import React from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';
import MainContainer from './components/MainContainer';

const Routes = () => (
    <Switch>
        <Route path="/" exact render={() => <Redirect to="/home" />} />
        <Route path="/home" exact component={MainContainer} />
    </Switch>
);

export default Routes;