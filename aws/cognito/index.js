const https = require('https');

exports.handler = (event, context, callback) => {
    const req = https.request(process.env.URL, res => {
        let body = '';
        console.log('Status:', res.statusCode);
        console.log('Headers:', JSON.stringify(res.headers));
        res.setEncoding('utf8');
        res.on('data', chunk => body += chunk);
        res.on('end', () => {
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
    req.write(JSON.stringify(event));
    req.end();

    callback(null, event);
};
