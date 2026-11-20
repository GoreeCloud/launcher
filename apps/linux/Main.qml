import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

ApplicationWindow {
    id: root
    visible: true
    width: 1080
    height: 720
    minimumWidth: 720
    minimumHeight: 500
    title: "GoreeCloud Launcher"
    color: "#0D121D"

    RowLayout {
        anchors.fill: parent
        spacing: 0

        Rectangle {
            Layout.preferredWidth: 230
            Layout.fillHeight: true
            color: "#171D2A"
            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 22
                spacing: 20
                Label { text: "◉  GoreeCloud"; font.pixelSize: 22; font.bold: true; color: "#F2F4FB" }
                Label { text: "LAUNCHER / LINUX"; color: "#BFB8FF"; font.pixelSize: 11; font.letterSpacing: 2 }
                Rectangle {
                    Layout.fillWidth: true; height: 48; radius: 16; color: "#413974"
                    Label { anchors.centerIn: parent; text: "⌂   Applications"; color: "#F2F4FB"; font.bold: true }
                }
                Item { Layout.fillHeight: true }
                Label { text: "Local application discovery"; color: "#CED5E5"; wrapMode: Text.Wrap }
                Label { text: "Qt Quick 6.12 · No telemetry"; color: "#9CA8BC"; font.pixelSize: 11 }
            }
        }

        Rectangle {
            Layout.fillWidth: true
            Layout.fillHeight: true
            color: "#0D121D"
            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 32
                spacing: 20

                RowLayout {
                    Layout.fillWidth: true
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 7
                        Label { text: "Your applications"; color: "#F2F4FB"; font.pixelSize: 30; font.bold: true }
                        Label { text: "Explore what is installed on this computer"; color: "#CED5E5"; font.pixelSize: 14 }
                    }
                    Button {
                        text: "Refresh"
                        onClicked: launcher.refresh()
                        Accessible.name: "Refresh installed applications"
                    }
                }
                TextField {
                    id: searchField
                    Layout.fillWidth: true
                    placeholderText: "Search applications…"
                    color: "#F2F4FB"
                    placeholderTextColor: "#9CA8BC"
                    background: Rectangle { color: "#252D3D"; border.color: "#505B70"; radius: 16 }
                    leftPadding: 18
                    topPadding: 14
                    bottomPadding: 14
                    onAccepted: {
                        if (appList.count > 0) launcher.launch(appList.model[0].id)
                    }
                    Accessible.name: "Search installed applications"
                }
                Label {
                    text: appList.count + " apps available"
                    color: "#CED5E5"
                    font.pixelSize: 13
                }
                ListView {
                    id: appList
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    spacing: 9
                    clip: true
                    model: launcher.entries.filter(function(entry) {
                        var term = searchField.text.toLowerCase().trim()
                        return entry.name.toLowerCase().indexOf(term) !== -1
                            || entry.description.toLowerCase().indexOf(term) !== -1
                    })
                    delegate: Rectangle {
                        required property var modelData
                        width: appList.width
                        height: 73
                        radius: 20
                        color: mouseArea.containsMouse ? "#333D52" : "#171D2A"
                        border.color: "#414C61"
                        RowLayout {
                            anchors.fill: parent
                            anchors.margins: 14
                            spacing: 16
                            Rectangle {
                                width: 46; height: 46; radius: 15; color: "#413974"
                                Label { anchors.centerIn: parent; text: modelData.name.slice(0,1).toUpperCase(); color: "#E8E5FF"; font.bold: true; font.pixelSize: 21 }
                            }
                            ColumnLayout {
                                Layout.fillWidth: true
                                Label { text: modelData.name; color: "#F2F4FB"; font.bold: true; elide: Text.ElideRight; Layout.fillWidth: true }
                                Label { text: modelData.description; color: "#CED5E5"; font.pixelSize: 12; elide: Text.ElideRight; Layout.fillWidth: true }
                            }
                            Label { text: "Open  ↗"; color: "#BFB8FF" }
                        }
                        MouseArea {
                            id: mouseArea
                            anchors.fill: parent
                            hoverEnabled: true
                            onClicked: launcher.launch(modelData.id)
                            Accessible.name: "Open " + modelData.name
                            Accessible.role: Accessible.Button
                        }
                    }
                    ScrollBar.vertical: ScrollBar {}
                }
                Label {
                    text: launcher.status
                    color: "#BFB8FF"
                    wrapMode: Text.Wrap
                    visible: text.length > 0
                    Accessible.name: launcher.status
                }
            }
        }
    }
}
