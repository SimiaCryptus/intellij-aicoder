
let socket;

function getSessionId() {
    if (!window.location.hash) {
        fetch('newSession')
            .then(response => {
                if (response.ok) {
                    return response.text();
                } else {
                    throw new Error('Failed to get new session ID');
                }
            })
            .then(sessionId => {
                window.location.hash = sessionId;
                connect(sessionId);
            });
    } else {
        return window.location.hash.substring(1);
    }
}

function send(message) {
    console.log('Sending message:', message);
    if (socket.readyState !== 1) {
        throw new Error('WebSocket is not open');
    }
    socket.send(message);
}

function connect(sessionId, customReceiveFunction) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const port = window.location.port;
    let path = window.location.pathname;
    let strings = path.split('/');
    if(strings.length >= 2 && strings[1] !== '' && strings[1] !== 'index.html') {
        path = '/' + strings[1] + '/';
    } else {
        path = '/';
    }

    socket = new WebSocket(`${protocol}//${host}:${port}${path}ws?sessionId=${sessionId}`);

    socket.addEventListener('open', (event) => {
        console.log('WebSocket connected:', event);
        showDisconnectedOverlay(false);
    });

    socket.addEventListener('message', customReceiveFunction || onWebSocketText);

    socket.addEventListener('close', (event) => {
        console.log('WebSocket closed:', event);
        showDisconnectedOverlay(true);
        setTimeout(() => {
            connect(getSessionId(), customReceiveFunction);
        }, 3000);
    });

    socket.addEventListener('error', (event) => {
        console.error('WebSocket error:', event);
    });
}

function showDisconnectedOverlay(show) {
    const elements = document.getElementsByClassName('ws-control');
    for (let i = 0; i < elements.length; i++) {
        elements[i].disabled = show;
    }
}
