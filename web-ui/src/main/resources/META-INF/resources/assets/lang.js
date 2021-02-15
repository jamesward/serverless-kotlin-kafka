const lang = window.location.pathname.slice(1);

const ws = new WebSocket('wss://stackoverflow-to-ws-x5ht4amjia-uc.a.run.app/questions');

ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    const tags = data.tags[0].split('|');

    if (tags.includes(lang)) {
        const template = document.getElementById('question-template');
        const question = document.importNode(template.content, true);

        for (const element of question.children) {
            for (const key in data) {
                element.innerHTML = element.innerHTML.replace(new RegExp('{{' + key + '}}'), data[key]);
            }
        }

        document.getElementById('questions').appendChild(question);
    }
}
