/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, { useEffect, useState } from 'react';
import type {PropsWithChildren} from 'react';
import {
  NativeModules,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  useColorScheme,
  View,
  Button,
  Alert,
  TextInput,
  FlatList,
  Modal,
  ActivityIndicator,
  Platform,
  PermissionsAndroid,
  Linking
} from 'react-native';
import ContactsClass from 'react-native-contacts';

import {
  Colors,
} from 'react-native/Libraries/NewAppScreen';

const { SMSModule } = NativeModules;
const { ContactsModule } = NativeModules;

interface Contact {
  phoneNumber: string;
  name: string;
}

interface Settings {
  triggerWord: string;
  allowedContacts: Contact[];
}

export default function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  const [loading, setLoading] = useState(false);
  const [settings, setSettings] = useState<Settings>({
    triggerWord: 'URGENT',
    allowedContacts: []
  });
  const [modalVisible, setModalVisible] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [deviceContacts, setDeviceContacts] = useState<any[]>([]);

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  useEffect(() => {
    console.log("Contacts:", ContactsClass);
    loadInitialData();
  }, []);

  const requestPermissions = async () => {
    try {
      if (Platform.OS === 'android') {
        await SMSModule.requestPermissions();
        // const permissions = [
        //   PermissionsAndroid.PERMISSIONS.READ_CONTACTS,
        //   PermissionsAndroid.PERMISSIONS.RECEIVE_SMS,
        //   PermissionsAndroid.PERMISSIONS.READ_SMS
        // ];

        // const results = await Promise.all(
        //   permissions.map(permission => 
        //     PermissionsAndroid.request(permission)
        //   )
        // );

        // if (results.some(result => result !== 'granted')) {
        //   throw new Error('Permissions not granted');
        // }
      }
    } catch (error) {
      console.error('Error requesting permissions:', error);
      Alert.alert(
        'Permissions Required',
        'This app needs contacts and SMS permissions to function properly.',
        [
          { text: 'OK', onPress: () => requestPermissions() },
          { text: 'Cancel', style: 'cancel' }
        ]
      );
    }
  };

  const loadSettings = async () => {
    try {
      const savedSettings = await SMSModule.getSettings();
      setSettings(savedSettings);
    } catch (error) {
      console.error('Error loading settings:', error);
    }
  };

  const loadContacts = async () => {
    try {
      const contacts = await ContactsModule.getContacts();
      setDeviceContacts(contacts);
    } catch (error) {
      console.error('Error loading contacts:', error);
      Alert.alert('Error', 'Failed to load contacts');
    }
  };

  const loadInitialData = () => {
    setLoading(true);
    requestPermissions()
      .then(() => {
        loadContacts();
        return loadSettings();
      })
      .catch((error) => {
        console.error('Error loading initial data:', error);
        Alert.alert('Error', 'Failed to initialize app');
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const saveSettings = async () => {
    setLoading(true);
    try {
      await SMSModule.updateSettings(settings.triggerWord, settings.allowedContacts);
      Alert.alert('Success', 'Settings saved successfully');
    } catch (error) {
      console.error('Error saving settings:', error);
      Alert.alert('Error', 'Failed to save settings');
    } finally {
      setLoading(false);
    }
  };

  const addContact = (contact: any) => {
    if (settings.allowedContacts.length >= 5) {
      Alert.alert('Limit Reached', 'Maximum 5 contacts allowed');
      return;
    }

    const phoneNumber = contact.phoneNumber.replace(/[^0-9]/g, '');
    if (!phoneNumber) {
      Alert.alert('Invalid Contact', 'Selected contact has no valid phone number');
      return;
    }

    const newContact: Contact = {
      name: contact.name || contact.displayName || contact.givenName || 'Unknown',
      phoneNumber
    };

    setSettings(prev => ({
      ...prev,
      allowedContacts: [...prev.allowedContacts, newContact]
    }));
    setModalVisible(false);
    setSearchQuery('');
  };

  const removeContact = (phoneNumber: string) => {
    setSettings(prev => ({
      ...prev,
      allowedContacts: prev.allowedContacts.filter(c => c.phoneNumber !== phoneNumber)
    }));
  };
  console.log("deviceContacts",deviceContacts)
  const filteredContacts = deviceContacts.filter(contact => {
    const searchLower = searchQuery.toLowerCase();
    const nameMatches = contact.name?.toLowerCase().includes(searchLower);
    const notAlreadyAdded = !settings.allowedContacts.some(c => 
      c.phoneNumber === contact.phoneNumber
    );
    return nameMatches && notAlreadyAdded;
});

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#0000ff" />
      </View>
    );
  }

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      {/* <TouchableOpacity onPress={() => NativeModules.ToastModule.show('Hello, world!', NativeModules.ToastModule.DURATION_SHORT)}>
        <View style={{
          padding: 10,
          backgroundColor: '#007AFF',
          borderRadius: 8,
          margin: 10
        }}>
          <Text style={{color: 'white'}}>Show Toast</Text>
        </View>
      </TouchableOpacity> */}
      <Text style={styles.title}>Emergency SMS Settings</Text>
      
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.section}>
          <Text style={styles.label}>Trigger Word:</Text>
          <TextInput
            style={styles.input}
            value={settings.triggerWord}
            onChangeText={text => setSettings(prev => ({ ...prev, triggerWord: text }))}
            placeholder="Enter trigger word"
          />
          <Text style={styles.hint}>
            Messages containing this word will trigger the emergency alert
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>
            Emergency Contacts ({settings.allowedContacts.length}/5):
          </Text>
          {settings.allowedContacts.map(contact => (
            <View key={contact.phoneNumber} style={styles.contactCard}>
              <View>
                <Text style={styles.contactName}>{contact.name}</Text>
                <Text style={styles.contactPhone}>{contact.phoneNumber}</Text>
              </View>
              <TouchableOpacity
                onPress={() => removeContact(contact.phoneNumber)}
                style={styles.removeButton}
              >
                <Text style={styles.removeButtonText}>Remove</Text>
              </TouchableOpacity>
            </View>
          ))}
          
          <TouchableOpacity
            style={[
              styles.addButton,
              settings.allowedContacts.length >= 5 && styles.addButtonDisabled
            ]}
            onPress={() => setModalVisible(true)}
            disabled={settings.allowedContacts.length >= 5}
          >
            <Text style={styles.addButtonText}>Add Contact</Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={styles.saveButton}
          onPress={saveSettings}
        >
          <Text style={styles.saveButtonText}>Save Settings</Text>
        </TouchableOpacity>
      </ScrollView>

      <Modal
        visible={modalVisible}
        animationType="slide"
        transparent={true}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Select Contact</Text>
            
            <TextInput
              style={styles.searchInput}
              value={searchQuery}
              onChangeText={setSearchQuery}
              placeholder="Search contacts..."
              autoFocus
            />

            <FlatList
              data={filteredContacts}
              keyExtractor={(item, index) => item.recordID || index.toString()}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={styles.contactListItem}
                  onPress={() => addContact(item)}
                >
                  <Text style={styles.contactListName}>
                    {item.name || item.displayName || item.givenName || 'Unknown'}
                  </Text>
                  {item.phoneNumber && (
                    <Text style={styles.contactListPhone}>
                      {item.phoneNumber}
                    </Text>
                  )}
                </TouchableOpacity>
              )}
              style={styles.contactsList}
            />

            <TouchableOpacity
              style={styles.closeButton}
              onPress={() => {
                setModalVisible(false);
                setSearchQuery('');
              }}
            >
              <Text style={styles.closeButtonText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      <TouchableOpacity 
        onPress={() => {
          SMSModule.testUrgentSMS();
        }}
        style={styles.testButton}
      >
        <Text style={styles.testButtonText}>Test SMS</Text>
      </TouchableOpacity>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollContent: {
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#333',
    textAlign: 'center',
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 15,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 10,
    color: '#333',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  hint: {
    fontSize: 12,
    color: '#666',
    marginTop: 5,
    fontStyle: 'italic',
  },
  contactCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#f8f8f8',
    padding: 12,
    borderRadius: 8,
    marginBottom: 10,
  },
  contactName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
  },
  contactPhone: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  removeButton: {
    padding: 8,
  },
  removeButtonText: {
    color: '#ff4444',
    fontSize: 14,
    fontWeight: '500',
  },
  addButton: {
    backgroundColor: '#007AFF',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 10,
  },
  addButtonDisabled: {
    backgroundColor: '#ccc',
  },
  addButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '500',
  },
  saveButton: {
    backgroundColor: '#4CD964',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 20,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  modalContainer: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
  },
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 12,
    padding: 20,
    maxHeight: '80%',
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 15,
    textAlign: 'center',
    color: '#333',
  },
  searchInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 15,
    fontSize: 16,
  },
  contactsList: {
    maxHeight: 400,
  },
  contactListItem: {
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  contactListName: {
    fontSize: 16,
    color: '#333',
  },
  contactListPhone: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  closeButton: {
    marginTop: 15,
    padding: 12,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    alignItems: 'center',
  },
  closeButtonText: {
    color: '#333',
    fontSize: 16,
    fontWeight: '500',
  },
  testButton: {
    backgroundColor: '#FF9500',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginHorizontal: 20,
    marginTop: 20,
  },
  testButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});

/*
  keytool -genkey -v -keystore test.keystore -alias test -keyalg RSA -keysize 2048 -validity 10000
*/