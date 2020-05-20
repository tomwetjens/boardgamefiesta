const https = require('https');

exports.handler = (event, context, callback) => {
    const url = process.env.URL;
    const options = {method: 'POST', headers: {'Content-Type': 'application/json'}};
    const requestBody = JSON.stringify(event);

    console.log('Requesting ' + options.method + ' ' + url + ':', requestBody);
    const req = https.request(url, options, res => {
        let body = '';
        console.log('Status:', res.statusCode);
        console.log('Headers:', JSON.stringify(res.headers));
        res.setEncoding('utf8');
        res.on('data', chunk => body += chunk);
        res.on('end', () => {
            if (res.statusCode >= 400) {
                console.error('Server returned error:', res.statusCode, body);
                callback(body, event);
                return;
            }

            console.log('Successfully processed HTTPS response');
            // If we know it's JSON, parse it
            if (res.headers['content-type'] === 'application/json') {
                event.response = JSON.parse(body);
            }

            // Return to Amazon Cognito
            callback(null, event);
        });
    });
    req.on('error', callback);
    req.write(requestBody);
    req.end();
};
