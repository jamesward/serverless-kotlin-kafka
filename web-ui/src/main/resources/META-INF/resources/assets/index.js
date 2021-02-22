const wsProto = (window.location.protocol === 'https:') ? 'wss:' : 'ws:';
const wsBase = `${wsProto}//${window.location.hostname}:${window.location.port}`;

window.addEventListener('load', () => {
    fetch('/total')
        .then(response => response.text())
        .then(body => {
            const template = document.getElementById('total-template');
            const total = document.importNode(template.content, true);
            const content = total.firstChild.textContent.replace('{{total}}', body);
            document.getElementById('total').innerHTML = content;
        });
});


function langsWS() {
    let ws = new WebSocket(`${wsBase}/langs`);

    ws.onmessage = function(event) {
        const [lang, num] = event.data.split(':');

        const template = document.getElementById('recent-questions-template');
        const recentQuestion = document.importNode(template.content, true);

        function replace(e) {
            return e.replaceAll('{{lang}}', lang).replaceAll('{{num}}', num);
        }

        for (const element of recentQuestion.children) {
            Array.from(element.attributes).forEach(attr => attr.value = replace(attr.value));
            element.innerHTML = replace(element.innerHTML);
        }

        const recentQuestions = document.getElementById('recent-questions');

        const existingRecentQuestion = document.getElementById(`lang-${lang}`);

        if (existingRecentQuestion != null) {
            existingRecentQuestion.replaceWith(recentQuestion);
        }
        else {
            recentQuestions.appendChild(recentQuestion);
        }
    }

    ws.onclose = function() {
        window.setTimeout(() => { ws = langsWS() }, 500);
    }
}

langsWS();
