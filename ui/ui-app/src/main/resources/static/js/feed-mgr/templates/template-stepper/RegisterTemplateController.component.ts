import * as _ from "underscore";
import {AccessControlService} from '../../../services/AccessControlService';
import {BroadcastService} from '../../../services/broadcast-service';
import { RegisterTemplateServiceFactory } from '../../services/RegisterTemplateServiceFactory';
import { Component, Inject, ViewChild, ViewEncapsulation } from '@angular/core';
import { StateService } from '@uirouter/core';
import { RegisterCompleteRegistrationController } from './register-template/register-template-step.component';
import { FormGroup, AbstractControl, FormControl, ValidatorFn } from '@angular/forms';
import '../../../../assets/images/75_arrow.svg'

@Component({
    selector:'register-template-controller',
    templateUrl: './register-template.html',
    styles: [`
        .mat-stepper-horizontal .mat-horizontal-content-container {
            background-color: #e0e0e0 !important;
        }
    `],
    encapsulation: ViewEncapsulation.None
})
export class RegisterTemplateController {

    /**
    * Reference to the RegisteredTemplate Kylo id passed when editing a template
    * @type {null|*}
    */
    registeredTemplateId: string;
    /**
    * Reference to the NifiTemplate Id. Used if kylo id above is not present
    * @type {null|*}
    */
    nifiTemplateId: string;
    /**
    * The model being edited/created
    */
    model: any;
    allowAccessControl: boolean = false;
    allowAdmin: boolean = false;
    allowEdit: boolean = false;
    /**
    * The Stepper Controller set after initialized
    * @type {null}
    */
    stepperController: any = null;
    loading: boolean = true;

    firstFormGroup: FormGroup = new FormGroup({});
    secondFormGroup: FormGroup = new FormGroup({});
    thirdFormGroup: FormGroup = new FormGroup({});
    fourthFormGroup: FormGroup = new FormGroup({});
    fifthFormGroup: FormGroup = new FormGroup({});

    isNew: boolean = false;

    constructor(private registerTemplateService: RegisterTemplateServiceFactory, 
                private stateService: StateService, 
                private accessControlService: AccessControlService, 
                private broadcastService: BroadcastService) {
        
        this.registeredTemplateId = this.stateService.params.registeredTemplateId || null;
        this.nifiTemplateId = this.stateService.params.nifiTemplateId || null;
        this.model = this.registerTemplateService.model;
        this.isNew = this.stateService.params.isNew || null;

        this.registerTemplateService.loadTemplateWithProperties(this.registeredTemplateId, this.nifiTemplateId).then((response: any) => {
            this.loading = false;
            this.registerTemplateService.warnInvalidProcessorNames();
            this.registerTemplateService.checkTemplateAccess().then((response: any) => {
                if (!response.isValid) {
                    //PREVENT access
                }
                this.allowAccessControl = response.allowAccessControl;
                this.allowAdmin = response.allowAdmin;
                this.allowEdit = response.allowEdit;
                this.broadcastService.notify("REGISTERED_TEMPLATE_LOADED", "LOADED");

            })
        }, (err: any) => {
            this.loading = false;
            this.registerTemplateService.resetModel();
            this.allowAccessControl = false;
            this.allowAdmin = false;
            this.allowEdit = false;
        });

        
    }
    
    cancelStepper () {
        this.registerTemplateService.resetModel();
        this.stateService.go("registered-templates");
    }

}