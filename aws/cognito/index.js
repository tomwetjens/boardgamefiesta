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
            // If we know it's JSON, parse it
            if (res.headers['content-type'] === 'application/json') {
                body = JSON.parse(body);
            }

            if (res.statusCode >= 400) {
                console.error('Error response:', res.statusCode, body);
                callback(body.message || body || res.statusCode, event);
                return;
            }

            console.log('Success response:', res.statusCode, body);

            // Return to Amazon Cognito
            if (body && body !== '') {
                event.response = body;
            }
            callback(null, event);
        });
    });
    req.on('error', callback);
    req.write(requestBody);
    req.end();
};
