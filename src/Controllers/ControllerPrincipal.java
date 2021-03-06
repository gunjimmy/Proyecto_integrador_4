 
package Controllers;

import Clases.Conexion_db;
import Clases.MensajeAlerta;
import Clases.Tabla;
import com.panamahitek.ArduinoException;
import com.panamahitek.PanamaHitek_Arduino;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.StageStyle;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;



public class ControllerPrincipal implements Initializable{
    @FXML
    private ComboBox<String> cbCaja;

    @FXML
    private DatePicker dtInicial;

    @FXML
    private Button btnConectar;

    @FXML
    private Button btnMostrar;

    @FXML
    private ComboBox<String> cbxTiempo;
    @FXML
    private Label lblEstatusError;

    @FXML
    private Button btnDesconectar;

    @FXML
    private Label lblEstatus;

    @FXML
    private DatePicker dtFinal;

    @FXML
    private TextArea txtVista;
    @FXML
    private Button btnGraficar;
    
    //para el llenado de las tablas en la base de datos
    @FXML
    private TableView tvTabla;
    @FXML
    private TableColumn<Tabla, Integer> col_id;
    @FXML
    private TableColumn<Tabla, String> col_fecha;
    @FXML
    private TableColumn<Tabla, String> col_hora;
    @FXML
    private TableColumn<Tabla, Float> col_humedad;
    
    MensajeAlerta al = new MensajeAlerta("Erro de conexión","Ha ocurrido un error con la conexión del servidor...Intente otra vez");
    //declaración de variables a utlilizar
    PanamaHitek_Arduino ino = new PanamaHitek_Arduino();
    
    ObservableList<String> ls =null;
    ObservableList<String> time= FXCollections.observableArrayList("1 hora","2 horas", "5 horas","1 día");
    ObservableList<Tabla> data = FXCollections.observableArrayList();
    //CONEXION DE LA BASE DE DATOS
    Connection conn =null;
    
    //escucha las acciones de arduuino
    SerialPortEventListener escucha = new SerialPortEventListener() {

        @Override
        public void serialEvent(SerialPortEvent spe) {
            try {
                if(ino.isMessageAvailable())
                {
                    //lo que se hace cuando recibe un mensaje por puerto serial
                    MostrarMensaje(ino.printMessage()); //mostramos lo que envia arduino en un cuadro de texto
                }
                } catch (SerialPortException ex) {
                lblEstatus.setText("Desconectado");
                lblEstatusError.setText("Fallo al recibir los datos");                  
                } catch (ArduinoException ex) {
                lblEstatus.setText("Desconectado");
                lblEstatusError.setText("Fallo del la conexion con arduino");   
                }
        }
    }; 
    
    private void MostrarMensaje(String msg) //muestra los datos recibidos en un panel
    {
        txtVista.appendText(msg+"\n");
    }
    
    private void MostrarPuertos() //muestra los puertos conectados
    {
        ls=FXCollections.observableArrayList();
        String[] portName = SerialPortList.getPortNames();
        for(String name:portName)
        {
            ls.add(name);
        }
        cbCaja.itemsProperty().setValue(ls);
    }
    
    @FXML //efectua la conexion con arduino
    private void ConectarArduino(ActionEvent event) {
        try 
        {
            ino.arduinoRX(cbCaja.getValue(), 9600, escucha);
            btnConectar.setDisable(true);
            btnDesconectar.setDisable(false);
            lblEstatus.setText("Conectado");
        } catch (ArduinoException ex) {
            btnConectar.setDisable(true);
            btnDesconectar.setDisable(false);
            lblEstatus.setText("Desconectado");
            lblEstatusError.setText("Fallo al conectar con arduino");
        } catch (SerialPortException ex) {
            
            btnConectar.setDisable(true);
            btnDesconectar.setDisable(false);
            lblEstatus.setText("Desconectado");
            lblEstatusError.setText("Fallo del puerto serial");
        }
    }

    @FXML //desabilita la conexion con arduino
    private void DesconectarArduino(ActionEvent event) {
        try 
        {
            ino.killArduinoConnection();
            btnConectar.setDisable(false);
            btnDesconectar.setDisable(true);
            lblEstatus.setText("Desconectado");
        } catch (ArduinoException ex) 
        {
            btnConectar.setDisable(true);
            btnDesconectar.setDisable(false);
            lblEstatus.setText("Conectado");
            lblEstatusError.setText("Fallo al desconectar la conecxión");
        }
    }
    @FXML
    private void mostrarFechHora()
    {
        Date fecha = new Date();
        DateFormat fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        System.out.println(fechaHora.format(fecha));
    }
    
    @FXML
    private void Resultados()
    {
        //String pattern="dd/MM/yyyy";
        //DateTimeFormatter d= DateTimeFormatter.ofPattern(pattern);
        //lblEstatusError.setText(dtInicial.getValue().format(d));
        LocalDate fechaInicio= dtInicial.getValue();
        LocalDate fechaFinal=dtFinal.getValue();
        if(fechaInicio==null || fechaFinal==null)
        {
            MensajeAlerta ms= new MensajeAlerta("Error de fecha","No has elegido una fecha..Ingrese una combinación de fechas válida");
            ms.MostrarMensaje();
        }
        else if( fechaInicio.isBefore(fechaFinal))
        {
            try {
                //fechaInicio es menor a la fecha2                         
                
                DateTimeFormatter dt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                 Statement sentencia = conn.createStatement();
                 System.out.println(dtInicial.getValue().format(dt));
                 ResultSet resultado = sentencia.executeQuery( "SELECT * FROM db_humedad WHERE fecha BETWEEN '"
                         +dtInicial.getValue().format(dt) +"' AND '"+dtFinal.getValue().format(dt)+"'");
                while(resultado.next())
                {
                   Tabla tbl = new Tabla();
                    tbl.setId(resultado.getInt("id"));
                    tbl.setFecha(resultado.getString("fecha"));
                    tbl.setHora(resultado.getString("hora"));
                    tbl.setDescripcion(resultado.getFloat("humedad"));
                    data.add(tbl);
                                    
                }
                tvTabla.setItems(data);
                //sentencia.close();
               
                col_id.setCellValueFactory(cellData -> cellData.getValue().getTablaid().asObject());
                col_fecha.setCellValueFactory(cellData -> cellData.getValue().getTablaFecha());
                col_humedad.setCellValueFactory(cellData -> cellData.getValue().getTablaHumedad().asObject());
                col_hora.setCellValueFactory(cellData -> cellData.getValue().getTablaHora());
                
                
            } catch (SQLException ex) 
            {
               al.MostrarMensaje();
            } 
        }
        else if(fechaInicio.isAfter(fechaFinal))
        {
            //fechaInicio es > a la primera fecha
            MensajeAlerta mg = new MensajeAlerta("Error de fecha","La combinación de fechas debe ser cronológica o iguales...");
            mg.MostrarMensaje();
        }
        else{
                
            try {
                //data=null;
                //tvTabla=null;
                Statement sentencia = conn.createStatement();
                ResultSet resultado = sentencia.executeQuery( "SELECT * FROM test" );
                while(resultado.next())
                {
                    Tabla tbl = new Tabla();
                    tbl.setId(resultado.getInt("id_test"));
                    tbl.setFecha(resultado.getString("fecha"));
                    tbl.setHora(resultado.getString("hora"));
                    tbl.setDescripcion(resultado.getFloat("humedad"));
                    data.add(tbl);
                }
                col_id.setCellValueFactory(cellData -> cellData.getValue().getTablaid().asObject());
                col_fecha.setCellValueFactory(cellData -> cellData.getValue().getTablaFecha());
                col_humedad.setCellValueFactory(cellData -> cellData.getValue().getTablaHumedad().asObject());
                col_hora.setCellValueFactory(cellData -> cellData.getValue().getTablaHora());
                tvTabla.setItems(data);
                //sentencia.close();
               
                
            } catch (SQLException ex) {
               
               al.MostrarMensaje();               
            } 
        }
        
    }

    @Override 
    public void initialize(URL location, ResourceBundle resources) {
        MostrarPuertos();
        btnDesconectar.setDisable(true);
        try {
            conn=Conexion_db.getConnection();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ControllerPrincipal.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ControllerPrincipal.class.getName()).log(Level.SEVERE, null, ex);
        }
        cbxTiempo.itemsProperty().setValue(time);
        
    }
}
