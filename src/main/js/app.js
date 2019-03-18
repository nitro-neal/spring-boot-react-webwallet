const React = require('react');
const ReactDOM = require('react-dom');
const Fingerprint2 = require('fingerprintjs2')
const QrCode = require('qrcode-generator')
var stompClient = require('./websocket-listener')

var that;
var fp;

var qrSize = 4;
var typeNumber = 4;
var errorCorrectionLevel = 'L';
var qr = QrCode(typeNumber, errorCorrectionLevel);

var isChromium = window.chrome;

class App extends React.Component {

	constructor(props) {
		super(props);
		this.handleSubmit = this.handleSubmit.bind(this);
		this.state = {message: '...', fingerprint: '...', receiveAddress: '...', balance: '...', transactions: [{"transactionType": "..", "address": "..", "timestamp": "..", "amount" : "..", "transactionId" : "..", "debug" : "..."}], hastransactions : false};
	}

	walletUpdate(payload) {
            console.log("Payload:" + payload)
            var payloadJson = JSON.parse(payload.body)

            that.setState({receiveAddress: payloadJson.receiveAddress});
            that.setState({balance: payloadJson.balance});

            if(payloadJson.transactions.length > 0){
                that.setState({hastransactions: true})
                that.setState({transactions: payloadJson.transactions})
            }

            qr.addData(payloadJson.receiveAddress);
            qr.make();
            document.getElementById('qrPlaceholder').innerHTML = qr.createImgTag(qrSize, qrSize * 4);
        }

    stompClientReady() {
        that.setState({fingerprint: fp});
        that.initWallet(fp);
    }

    initWallet(fingerprint) {
        fetch('/initwallet', {
          method: "GET",
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Fingerprint': fingerprint
          }

          }).then(function(response) {
              console.log(response.status);
          }, function(error) {
              console.log(error.message);
          });
    }

    handleSubmit(event) {
        event.preventDefault();
        //const data = new FormData(event.target);

        var data = JSON.stringify({"amount": event.target[0].value, "address": event.target[1].value});
        console.log("ABOUT TO SEND: " + data)

        fetch('/sendCoins', {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Fingerprint': fp
          },
          body: data,
        }).then(function(response) {
            console.log(response.status);
        }, function(error) {
            console.log(error.message);
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

                  // TODO: DEBUGGING
                  fp = murmur;
//                  if(isChromium) {
//                    fp = murmur;
//                  } else {
//                    fp = ("" + Math.random()).replace(".","");
//                  }

                  stompClient.register([
                      {route: '/topic/updateWallet-' + fp, callback: that.walletUpdate}
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

                   // TODO: DEBUGGING
                    fp = murmur;
//                    if(isChromium) {
//                        fp = murmur;
//                    } else {
//                        fp = "" + Math.random();
//                    }

                  stompClient.register([
                      {route: '/topic/updateWallet-' + fp, callback: that.walletUpdate}
                  ], that.stompClientReady);
                })
            }, 500)
        }
	}

	render() {
		return (
          <div>
              
            <div class="content-center">
                <h1>Bit Burn Wallet</h1>
                <h3> {this.state.balance} BTCt</h3>
                <button type="button" class="btn btn-lg btn-primary" data-toggle="modal" data-target="#sendModal">Send   </button>
                <button type="button" class="btn btn-lg btn-primary" data-toggle="modal" data-target="#receiveModal">Receive</button>
                
                <br/>
                <br/>
                <br/>
                    
                {this.state.transactions.map((t) => <div class="row"> <div class="col"><span> {t.transactionType} </span></div> <div class="col"><span>{t.amount}</span></div></div>)}

            </div>
            
            <div class="modal fade" id="sendModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
                <div class="modal-dialog" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="exampleModalLabel">Send BTCt</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <form onSubmit={this.handleSubmit}>
                                <div class="form-group">
                                    <label class ="blackcolor" htmlFor="amount">Amount</label>
                                    <input name="amount" type="text" class="form-control blackcolor" id="amount" aria-describedby="emailHelp" placeholder="Enter amount"/>
                                </div>
                                <div class="form-group">
                                    <label class ="blackcolor" htmlFor="address">Address</label>
                                    <input name="address" type="text" class="form-control blackcolor" id="address" aria-describedby="emailHelp" placeholder="Enter address"/>
                                </div>
                                <button type="submit" class="btn btn-primary">Send</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>

            <div class="modal fade" id="receiveModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
                <div class="modal-dialog" role="document">
                    <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="exampleModalLabel">Receive BTCt</h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <div class="alert alert-primary" role="alert">
                            ONLY SEND TESTNET BITCOINS TO THIS ADDRESS!
                        </div>
                        <p> Receive Address: {this.state.receiveAddress} </p>
                        <div id="qrPlaceholder"></div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                    </div>
                    </div>
                </div>
            </div>
          </div>
		)
	}
}

ReactDOM.render(
	<App />,
	document.getElementById('react')
)