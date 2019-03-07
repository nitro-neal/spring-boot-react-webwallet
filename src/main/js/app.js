const React = require('react');
const ReactDOM = require('react-dom');
const Fingerprint2 = require('fingerprintjs2')
const client = require('./client');

var stompClient = require('./websocket-listener')
var that;
var fp;

class App extends React.Component {

	constructor(props) {
		super(props);
		this.handleSubmit = this.handleSubmit.bind(this);
		this.state = {message: '...', fingerprint: '...', receiveAddress: '...', balance: '...'};
	}

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

    handleSubmit(event) {
        event.preventDefault();
        //const data = new FormData(event.target);

        var data = JSON.stringify({"amount": event.target[0].value, "address": event.target[1].value});
        console.log("ABOUT TO SEND: " + data)

        //fetch('/api/form-submit-url', {
        fetch('http://localhost:8080/sendCoins', {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Fingerprint': fp
          },
          body: data,
        }).then(function(response) {
            console.log(response.status);     //=> number 100–599
        }, function(error) {
            console.log(error.message); //=> String
        });
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
            <p> Balance : {this.state.balance} </p>
            <br/>

            <form onSubmit={this.handleSubmit}>
                <label htmlFor="amount">Enter amount</label>
                <input id="amount" name="amount" type="text" />

                <label htmlFor="address">Enter address</label>
                <input id="address" name="address" type="text" />

                <button>Send data!</button>
            </form>
          </div>
		)
	}
}

ReactDOM.render(
	<App />,
	document.getElementById('react')
)