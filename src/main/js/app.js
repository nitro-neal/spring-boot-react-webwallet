const React = require('react');
const ReactDOM = require('react-dom');
const Fingerprint2 = require('fingerprintjs2')
const client = require('./client');

var stompClient = require('./websocket-listener')
var that;
var fp;

class App extends React.Component {

    walletUpdate(payload) {
        console.log("Walletupdate called with MYPAYLOAD:" + payload)
        var payloadJson = JSON.parse(payload.body)

        that.setState({receiveAddress: payloadJson.receiveAddress});
        that.setState({balance: payloadJson.balance});
    }

    stompClientReady() {
        that.setState({fingerprint: fp});
        that.initWallet(fp);
    }

    initWallet(fingerprint) {
        fetch('http://localhost:8080/initwallet', {
          method: "GET",
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Fingerprint': fingerprint
          }

          }).then(function(response) {
              console.log(response.status);     //=> number 100–599
          }, function(error) {
              console.log(error.message); //=> String
        });
    }

	constructor(props) {
		super(props);
		this.state = {message: '...', fingerprint: '...', receiveAddress: '...', balance: '...'};
	}

    //componentDidMount is the API invoked after React renders a component in the DOM.
	componentDidMount() {
        that = this;

	    if (window.requestIdleCallback) {
            requestIdleCallback(function () {
                Fingerprint2.get(function (components) {
                  console.log(components);
                  var values = components.map(function (component) { return component.value });
                  var murmur = Fingerprint2.x64hash128(values.join(''), 31);
                  console.log(murmur);

                  fp = murmur;

                  stompClient.register([
                      {route: '/topic/updateWallet-' + murmur, callback: that.walletUpdate}
                  ], that.stompClientReady);

                })
            })
        } else {
            setTimeout(function () {
                Fingerprint2.get(function (components) {
                  console.log(components) // an array of components: {key: ..., value: ...}
                  var values = components.map(function (component) { return component.value })
                  var murmur = Fingerprint2.x64hash128(values.join(''), 31)
                  console.log(murmur) // an array of components: {key: ..., value: ...}

                  fp = murmur;

                  stompClient.register([
                      {route: '/topic/updateWallet-' + murmur, callback: that.walletUpdate}
                  ], that.stompClientReady);
                })
            }, 500)
        }
	}

	render() {
		return (
          <div>
            <p> Message: {this.state.message} </p>
            <p> Fingerprint: {this.state.fingerprint} </p>
            <p> Receive Address : {this.state.receiveAddress} </p>
            <p> Receive Address : {this.state.balance} </p>
            <br/>



          </div>
		)
	}
}

ReactDOM.render(
	<App />,
	document.getElementById('react')
)