function getSelection() {
    if(document.getSelection){
        var selection = document.getSelection().toString();
        return selection;
    } else {
        return null;
    }
}

getSelection();
