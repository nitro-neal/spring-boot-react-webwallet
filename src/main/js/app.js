const React = require('react');
const ReactDOM = require('react-dom');
const client = require('./client');

class App extends React.Component {

	constructor(props) {
		super(props);
		this.state = {message: 'w'};
	}

    //componentDidMount is the API invoked after React renders a component in the DOM.
	componentDidMount() {
        fetch('http://localhost:8080/setfingerprint', {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            fingerprint: '123'
          })
        }).then(response => {
            console.log(response)
            client({method: 'GET', path: '/initwallet'}).done(response => {
                console.log(response)
                this.setState({message: response.entity.message});
            });
        });
	}

	render() {
		return (
          <div>
            Message: {this.state.message}
          </div>
		)
	}
}

ReactDOM.render(
	<App />,
	document.getElementById('react')
)