const totalWS = new WebSocket("ws://localhost:8080/total");

totalWS.onmessage = function(event) {
    const template = document.getElementById('totalTemplate');
    const total = document.importNode(template.content, true);
    const content = total.firstChild.textContent.replace("{{total}}", event.data);
    document.getElementById('total').innerHTML = content;
}

const langsWS = new WebSocket("ws://localhost:8080/langs");

langsWS.onmessage = function(event) {
    console.log(event);
}
