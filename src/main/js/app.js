const React = require('react');
const ReactDOM = require('react-dom');
const Fingerprint2 = require('fingerprintjs2')
const client = require('./client');

var that;

class App extends React.Component {

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
              console.log(response.statusText); //=> String
              console.log(response.headers);    //=> Headers
              console.log(response.url);        //=> String
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
                  console.log(components) // an array of components: {key: ..., value: ...}
                  var values = components.map(function (component) { return component.value })
                  var murmur = Fingerprint2.x64hash128(values.join(''), 31)
                  console.log(murmur) // an array of components: {key: ..., value: ...}
                  that.setState({fingerprint: murmur},() => {
                    that.initWallet(murmur);
                  })
                })
            })
        } else {
            setTimeout(function () {
                Fingerprint2.get(function (components) {
                  console.log(components) // an array of components: {key: ..., value: ...}
                  var values = components.map(function (component) { return component.value })
                  var murmur = Fingerprint2.x64hash128(values.join(''), 31)
                  console.log(murmur) // an array of components: {key: ..., value: ...}
                  that.setState({fingerprint: murmur},() => {
                    that.initWallet(murmur);
                  })
                })
            }, 500)
        }
	}

    balanceClick(fingerprint) {
        fetch('http://localhost:8080/getBalance', {
          method: "GET",
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Fingerprint': fingerprint
          }

          })
          .then(response => response.json())
          .then(function(response) {
              console.log(response);
              that.setState({balance: response.balance});
          }, function(error) {
              console.log(error.message);
        });
    }

    receiveClick(fingerprint) {
        fetch('http://localhost:8080/getReceiveAddress', {
          method: "GET",
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Fingerprint': fingerprint
          }

          })
          .then(response => response.json())
          .then(function(response) {
              console.log(response);
              that.setState({receiveAddress: response.receiveAddress});
          }, function(error) {
              console.log(error.message);
        });
    }

	render() {
		return (
          <div>
            <p> Message: {this.state.message} </p>
            <p> Fingerprint: {this.state.fingerprint} </p>
            <p> Receive Address : {this.state.receiveAddress} </p>
            <p> Receive Address : {this.state.balance} </p>
            <br/>

             <button onClick = {
                      this.receiveClick.bind(null, this.state.fingerprint)
                  } > Receive TBTC
              </button>

              <br/>

               <button onClick = {
                        this.balanceClick.bind(null, this.state.fingerprint)
                    } > Receive Balance
                </button>

          </div>
		)
	}
}

ReactDOM.render(
	<App />,
	document.getElementById('react')
)